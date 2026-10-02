package li.cil.oc.util;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * R-Tree over 3D points. The Scala implicit coordinate function
 * {@code Data => (Double, Double, Double)} is an explicit constructor
 * argument returning a {@link Triple}.
 */
public class RTree<Data> {
    private final int M;
    public final Function<Data, Triple<Double, Double, Double>> coordinate;

    // Used for quick checks whether values are in the tree, e.g. for updates.
    private final Map<Data, Leaf> entries = new HashMap<>();

    private final int m;

    private NonLeaf root = new NonLeaf();

    public RTree(int M, Function<Data, Triple<Double, Double, Double>> coordinate) {
        if (M < 2) throw new IllegalArgumentException("maxEntries must be larger or equal to 2.");
        this.M = M;
        this.coordinate = coordinate;
        this.m = Math.max(M / 2, 1);
    }

    public synchronized Optional<Triple<Double, Double, Double>> apply(Data value) {
        final Leaf position = entries.get(value);
        if (position == null) return Optional.empty();
        return Optional.of(position.bounds().min.asTuple());
    }

    // Allows debug rendering of the tree.
    public synchronized List<Pair<Pair<Triple<Double, Double, Double>, Triple<Double, Double, Double>>, Integer>> allBounds() {
        return root.allBounds(0);
    }

    public synchronized boolean add(Data value) {
        final boolean replaced = remove(value);
        final Leaf entry = new Leaf(value, new Point(coordinate.apply(value)));
        entries.put(value, entry);
        final Node newNode = root.add(entry);
        if (newNode != root) {
            root = new NonLeaf(newNode, root);
        }
        return !replaced;
    }

    public synchronized boolean remove(Data value) {
        final Leaf node = entries.remove(value);
        if (node != null) {
            final Optional<Node> change = root.remove(node);
            assert change.isPresent() && (change.get() == node || change.get() == root);
            final Node head = root.children.isEmpty() ? null : root.children.iterator().next();
            if (head instanceof NonLeaf nonLeaf && root.children.size() == 1) {
                root = nonLeaf;
            } else {
                root.bounds = Rectangle.around(root.children);
            }
            return true;
        }
        return false;
    }

    public synchronized List<Data> query(Triple<Double, Double, Double> from, Triple<Double, Double, Double> to) {
        return root.query(new Rectangle(new Point(from), new Point(to)));
    }

    private abstract class Node {
        abstract Rectangle bounds();

        List<Pair<Pair<Triple<Double, Double, Double>, Triple<Double, Double, Double>>, Integer>> allBounds(int level) {
            final List<Pair<Pair<Triple<Double, Double, Double>, Triple<Double, Double, Double>>, Integer>> result = new ArrayList<>();
            result.add(Pair.of(bounds().asTuple(), level));
            return result;
        }

        boolean isLeaf() {
            return true;
        }

        abstract Node add(Node value);

        abstract Optional<Node> remove(Node value);

        abstract List<Data> query(Rectangle query);
    }

    private class NonLeaf extends Node {
        final Set<Node> children = new LinkedHashSet<>();

        Rectangle bounds = new Rectangle(Point.PositiveInfinity, Point.NegativeInfinity);

        NonLeaf() {
        }

        NonLeaf(Node... nodes) {
            for (Node child : nodes) {
                children.add(child);
                bounds = bounds.including(child.bounds());
            }
        }

        @Override
        Rectangle bounds() {
            return bounds;
        }

        @Override
        List<Pair<Pair<Triple<Double, Double, Double>, Triple<Double, Double, Double>>, Integer>> allBounds(int level) {
            final List<Pair<Pair<Triple<Double, Double, Double>, Triple<Double, Double, Double>>, Integer>> result = super.allBounds(level);
            for (Node child : children) {
                result.addAll(child.allBounds(level + 1));
            }
            return result;
        }

        @Override
        boolean isLeaf() {
            return !children.isEmpty() && children.iterator().next() instanceof RTree<?>.Leaf;
        }

        @Override
        Node add(Node value) {
            assert value != this;
            uncheckedAdd(value);
            if (children.size() > M) {
                return split();
            } else {
                bounds = bounds.including(value.bounds());
                return this;
            }
        }

        private void uncheckedAdd(Node value) {
            Node bestChild = null;
            double bestGrowth = Double.POSITIVE_INFINITY;
            double bestVolume = Double.POSITIVE_INFINITY;
            for (Node child : children) {
                if (!(!child.isLeaf() || value instanceof RTree<?>.Leaf)) continue;
                final double oldVolume = child.bounds().volume();
                final double volume = child.bounds().including(value.bounds()).volume();
                final double growth = volume - oldVolume;
                if (growth < bestGrowth || (growth == bestGrowth && volume < bestVolume)) {
                    bestChild = child;
                    bestGrowth = growth;
                    bestVolume = volume;
                }
            }
            if (bestChild != null) {
                children.add(bestChild.add(value));
            } else {
                // Empty root or node while inserting children of removing child node.
                children.add(value);
            }
        }

        @Override
        Optional<Node> remove(Node value) {
            if (bounds.intersects(value.bounds())) {
                for (Node child : new ArrayList<>(children)) {
                    final Optional<Node> result = child.remove(value);
                    if (result.isPresent()) {
                        final Node change = result.get();
                        if (change == child) {
                            // Underflow after removing node or child was the node to remove.
                            children.remove(child);
                            if (child instanceof RTree<?>.NonLeaf) {
                                @SuppressWarnings("unchecked") final NonLeaf node = (NonLeaf) child;
                                for (Node grandChild : node.children) {
                                    uncheckedAdd(grandChild);
                                }
                                if (children.size() > M) {
                                    // Escalate overflow.
                                    return Optional.of(split());
                                }
                            } else {
                                assert child == value;
                            }
                            if (children.size() < m) {
                                // Escalate underflow.
                                return Optional.of(this);
                            }
                            // Done handling tree adjustment, bubble result up.
                            bounds = Rectangle.around(children);
                            return Optional.of(value);
                        } else if (change == value) {
                            // Removal, bubble result up.
                            bounds = Rectangle.around(children);
                            return Optional.of(value);
                        } else {
                            // Overflow due to split after underflow.
                            assert change instanceof RTree<?>.NonLeaf;
                            uncheckedAdd(change);
                            if (children.size() > M) {
                                // Escalate overflow.
                                return Optional.of(split());
                            } else {
                                // Done handling tree adjustment, bubble result up.
                                bounds = Rectangle.around(children);
                                return Optional.of(value);
                            }
                        }
                    }
                }
            }
            return Optional.empty();
        }

        @Override
        List<Data> query(Rectangle query) {
            if (query.intersects(bounds)) {
                final List<Data> result = new ArrayList<>();
                for (Node child : children) {
                    result.addAll(child.query(query));
                }
                return result;
            } else return Collections.emptyList();
        }

        private NonLeaf split() {
            final List<Node> values = new ArrayList<>(children);
            Node seed1 = null;
            Node seed2 = null;
            double worst = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < values.size(); i++) {
                final Node si = values.get(i);
                for (int j = i + 1; j < values.size(); j++) {
                    final Node sj = values.get(j);
                    final double vol1 = si.bounds().volume();
                    final double vol2 = sj.bounds().volume();
                    final double vol = si.bounds().including(sj.bounds()).volume();
                    final double d = vol - vol1 - vol2;
                    if (d > worst) {
                        seed1 = si;
                        seed2 = sj;
                        worst = d;
                    }
                }
            }
            if (seed1 == null || seed2 == null) throw new AssertionError();

            final SplitResult r1 = new SplitResult(seed1);
            final SplitResult r2 = new SplitResult(seed2);

            final Set<Node> list = new LinkedHashSet<>(values);
            list.remove(seed1);
            list.remove(seed2);
            while (!list.isEmpty()) {
                if (m - r1.set.size() >= list.size()) {
                    list.forEach(r1::add);
                    list.clear();
                } else if (m - r2.set.size() >= list.size()) {
                    list.forEach(r2::add);
                    list.clear();
                } else {
                    Node bestValue = null;
                    SplitResult r = r1;
                    double best = Double.NEGATIVE_INFINITY;
                    for (Node value : list) {
                        final double newVol1 = r1.volumeIncluding(value);
                        final double newVol2 = r2.volumeIncluding(value);
                        final double growth1 = newVol1 - r1.volume();
                        final double growth2 = newVol2 - r2.volume();
                        final double d = Math.abs(growth2 - growth1);
                        if (d > best) {
                            bestValue = value;
                            r = (growth1 < growth2 || (growth1 == growth2 && newVol1 < newVol2)) ? r1 : r2;
                            best = d;
                        }
                    }
                    if (bestValue == null) throw new AssertionError();
                    list.remove(bestValue);
                    r.add(bestValue);
                }
            }

            children.clear();
            children.addAll(r1.set);
            bounds = r1.bounds;

            final NonLeaf LL = new NonLeaf();
            LL.children.addAll(r2.set);
            LL.bounds = r2.bounds;
            return LL;
        }
    }

    private class Leaf extends Node {
        final Data data;
        private final Rectangle bounds;

        Leaf(Data data, Point point) {
            this.data = data;
            this.bounds = new Rectangle(point, point);
        }

        @Override
        Rectangle bounds() {
            return bounds;
        }

        @Override
        Node add(Node value) {
            return value;
        }

        @Override
        Optional<Node> remove(Node value) {
            if (value == this) return Optional.of(this);
            else return Optional.empty();
        }

        @Override
        List<Data> query(Rectangle query) {
            if (query.intersects(bounds)) return Collections.singletonList(data);
            else return Collections.emptyList();
        }
    }

    private static final class Point {
        static final Point NegativeInfinity = new Point(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        static final Point PositiveInfinity = new Point(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

        final double x;
        final double y;
        final double z;

        Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Point(Triple<Double, Double, Double> p) {
            this(p.getLeft(), p.getMiddle(), p.getRight());
        }

        Point min(Point other) {
            return new Point(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
        }

        Point max(Point other) {
            return new Point(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
        }

        Triple<Double, Double, Double> asTuple() {
            return Triple.of(x, y, z);
        }
    }

    private static final class Rectangle {
        final Point min;
        final Point max;

        Rectangle(Point min, Point max) {
            this.min = min;
            this.max = max;
        }

        Rectangle including(Rectangle value) {
            return new Rectangle(value.min.min(min), value.max.max(max));
        }

        boolean intersects(Rectangle value) {
            return value.min.x <= max.x && value.min.y <= max.y && value.min.z <= max.z &&
                    value.max.x >= min.x && value.max.y >= min.y && value.max.z >= min.z;
        }

        double volume() {
            final double sx = max.x - min.x;
            final double sy = max.y - min.y;
            final double sz = max.z - min.z;
            return sx * sy * sz;
        }

        Pair<Triple<Double, Double, Double>, Triple<Double, Double, Double>> asTuple() {
            return Pair.of(Triple.of(min.x, min.y, min.z), Triple.of(max.x, max.y, max.z));
        }

        static Rectangle around(Iterable<? extends RTree<?>.Node> values) {
            Point min = Point.PositiveInfinity;
            Point max = Point.NegativeInfinity;
            for (RTree<?>.Node value : values) {
                min = value.bounds().min.min(min);
                max = value.bounds().max.max(max);
            }
            return new Rectangle(min, max);
        }
    }

    private class SplitResult {
        final Set<Node> set = new LinkedHashSet<>();
        Rectangle bounds;

        SplitResult(Node seed) {
            set.add(seed);
            bounds = seed.bounds();
        }

        void add(Node value) {
            set.add(value);
            bounds = bounds.including(value.bounds());
        }

        double volume() {
            return bounds.volume();
        }

        double volumeIncluding(Node value) {
            return bounds.including(value.bounds()).volume();
        }
    }
}
