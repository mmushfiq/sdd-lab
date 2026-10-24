package ch.martinelli.edu.hrs.management;

/**
 * The new block overlaps another block of the same room (UC-018 A2, BR-001).
 */
public class RoomBlockOverlapException extends RuntimeException {

    private final transient RoomBlockOverview overlappingBlock;

    public RoomBlockOverlapException(RoomBlockOverview overlappingBlock) {
        super("The block overlaps the block " + overlappingBlock.id());
        this.overlappingBlock = overlappingBlock;
    }

    public RoomBlockOverview overlappingBlock() {
        return overlappingBlock;
    }
}
