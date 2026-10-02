package li.cil.oc.server.network;

public interface Distributor {
    double globalBuffer();

    void setGlobalBuffer(double value);

    double globalBufferSize();

    void setGlobalBufferSize(double value);

    void addConnector(Connector connector);

    void removeConnector(Connector connector);

    double changeBuffer(double delta);
}
