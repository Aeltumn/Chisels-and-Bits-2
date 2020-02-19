package nl.dgoossens.chiselsandbits2.common.impl.item;

import net.minecraft.util.Direction;
import nl.dgoossens.chiselsandbits2.ChiselsAndBits2;
import nl.dgoossens.chiselsandbits2.api.item.IMenuAction;
import nl.dgoossens.chiselsandbits2.common.network.client.CRotateItemPacket;
import nl.dgoossens.chiselsandbits2.client.util.ClientItemPropertyUtil;

public enum MenuAction implements IMenuAction {
    //General
    UNDO,
    REDO,

    //Chisel
    PLACE,
    SWAP,

    //Chiseled Block / Pattern
    ROLL_X,
    ROLL_Y,
    ROLL_Z,
    ;

    @Override
    public boolean hasIcon() {
        return true;
    }

    @Override
    public boolean hasHotkey() {
        return true;
    }

    @Override
    public void trigger() {
        boolean sneaking = ChiselsAndBits2.getInstance().getClient().getPlayer().isCrouching();
        switch (this) {
            case UNDO:
                ChiselsAndBits2.getInstance().getUndoTracker().undo();
                break;
            case REDO:
                ChiselsAndBits2.getInstance().getUndoTracker().redo();
                break;
            case ROLL_X:
                ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CRotateItemPacket(Direction.Axis.X, !sneaking));
                break;
            case ROLL_Y:
                ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CRotateItemPacket(Direction.Axis.Y, !sneaking));
                break;
            case ROLL_Z:
                ChiselsAndBits2.getInstance().getNetworkRouter().sendToServer(new CRotateItemPacket(Direction.Axis.Z, !sneaking));
                break;
            case PLACE:
            case SWAP:
                ClientItemPropertyUtil.setSwapState(this.equals(MenuAction.SWAP));
                break;
        }
    }
}
