package li.cil.oc.api.fs;

import li.cil.oc.api.Persistable;

/**
 * Used by file system components to get and set the file system's label.
 *
 * @see li.cil.oc.api.FileSystem#asManagedEnvironment
 */
public interface Label extends Persistable {
    /**
     * Get the current value of this label.
     * <br>
     * May be {@code null} if no label is set.
     *
     * @return the current label.
     */
    String getLabel();

    /**
     * Set the new value of this label.
     * <br>
     * May be set to {@code null} to clear the label.
     * <br>
     * May throw an exception if the label is read-only.
     *
     * @param value the new label.
     * @throws IllegalArgumentException if the label is read-only.
     */
    void setLabel(String value);
}
