package ez.pogdog.yescom.data;

public interface ISerializable {

    /**
     * Returns the serializer responsible for serializing this.
     * @return The serializer.
     */
    ISerializer getSerializer();
}
