package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapleControllerTest {

    private static int hostAddress(int port) {
        return MapleAddress.host(port);
    }

    private static int controllerAddress(int port) {
        return MapleAddress.encode(port, true, 0);
    }

    private static byte[] funcParam(int func) {
        return new byte[] {(byte) (func >>> 24), (byte) (func >>> 16), (byte) (func >>> 8), (byte) func};
    }

    private static MapleFrameHeader decodeResponseHeader(byte[] response) {
        int word = ((response[0] & 0xFF) << 24) | ((response[1] & 0xFF) << 16)
                | ((response[2] & 0xFF) << 8) | (response[3] & 0xFF);
        return MapleFrameHeader.decode(word);
    }

    @Test
    void requestDeviceInfoReturnsTheControllersDeviceInfoBlock() {
        int port = 0;
        MapleController controller = MapleController.standard(controllerAddress(port));
        MapleFrameHeader request = new MapleFrameHeader(
                MapleCommand.REQUEST_DEVICE_INFO, controller.address(), hostAddress(port), 0);

        byte[] response = controller.handleRequest(request, new byte[0]);
        MapleFrameHeader responseHeader = decodeResponseHeader(response);
        byte[] responseData = Arrays.copyOfRange(response, 4, response.length);

        assertEquals(MapleCommand.DEVICE_INFO, responseHeader.command());
        assertEquals(hostAddress(port), responseHeader.recipientAddress()); // sent back to whoever asked
        assertEquals(controller.address(), responseHeader.senderAddress()); // from the controller itself
        assertEquals(28, responseHeader.additionalWordCount()); // 112 bytes / 4
        MapleDeviceInfo decodedInfo = MapleDeviceInfo.decode(responseData, 0);
        assertEquals(MapleFunctionCode.CONTROLLER, decodedInfo.functionCodes());
    }

    @Test
    void getConditionForControllerFunctionReturnsFuncThenCondition() {
        int port = 1;
        MapleController controller = MapleController.standard(controllerAddress(port));
        ControllerCondition pressed = ControllerCondition.neutral().withButton(ControllerButton.A, true);
        controller.setCondition(pressed);
        MapleFrameHeader request = new MapleFrameHeader(
                MapleCommand.GET_CONDITION, controller.address(), hostAddress(port), 1);

        byte[] response = controller.handleRequest(request, funcParam(MapleFunctionCode.CONTROLLER));
        MapleFrameHeader responseHeader = decodeResponseHeader(response);
        byte[] responseData = Arrays.copyOfRange(response, 4, response.length);

        assertEquals(MapleCommand.DATA_TRANSFER, responseHeader.command());
        assertEquals(hostAddress(port), responseHeader.recipientAddress());
        assertEquals(controller.address(), responseHeader.senderAddress());
        assertEquals(3, responseHeader.additionalWordCount()); // 1 (func) + 2 (8-byte cond)
        assertEquals(12, responseData.length);

        byte[] echoedFunc = Arrays.copyOfRange(responseData, 0, 4);
        assertArrayEquals(funcParam(MapleFunctionCode.CONTROLLER), echoedFunc);

        ControllerCondition decodedCondition = ControllerCondition.decode(responseData, 4);
        assertEquals(pressed, decodedCondition);
    }

    @Test
    void getConditionForUnsupportedFunctionReturnsFunctionCodeUnsupported() {
        int port = 2;
        MapleController controller = MapleController.standard(controllerAddress(port));
        MapleFrameHeader request = new MapleFrameHeader(
                MapleCommand.GET_CONDITION, controller.address(), hostAddress(port), 1);

        byte[] response = controller.handleRequest(request, funcParam(MapleFunctionCode.MEMORY_CARD));
        MapleFrameHeader responseHeader = decodeResponseHeader(response);

        assertEquals(MapleCommand.FUNCTION_CODE_UNSUPPORTED, responseHeader.command());
        assertEquals(0, responseHeader.additionalWordCount());
        assertEquals(4, response.length); // header only, no data
    }

    @Test
    void unhandledCommandReturnsUnknownCommandRatherThanBeingSilentlyIgnored() {
        int port = 0;
        MapleController controller = MapleController.standard(controllerAddress(port));
        MapleFrameHeader request = new MapleFrameHeader(
                MapleCommand.RESET_DEVICE, controller.address(), hostAddress(port), 0);

        byte[] response = controller.handleRequest(request, new byte[0]);
        MapleFrameHeader responseHeader = decodeResponseHeader(response);

        assertEquals(MapleCommand.UNKNOWN_COMMAND, responseHeader.command());
    }

    @Test
    void getConditionWithMissingFuncParameterThrows() {
        int port = 0;
        MapleController controller = MapleController.standard(controllerAddress(port));
        MapleFrameHeader request = new MapleFrameHeader(
                MapleCommand.GET_CONDITION, controller.address(), hostAddress(port), 0);

        assertThrows(IllegalArgumentException.class, () -> controller.handleRequest(request, new byte[0]));
    }

    @Test
    void setConditionIsReflectedInTheNextGetConditionResponse() {
        int port = 3;
        MapleController controller = MapleController.standard(controllerAddress(port));
        MapleFrameHeader request = new MapleFrameHeader(
                MapleCommand.GET_CONDITION, controller.address(), hostAddress(port), 1);

        ControllerCondition first = ControllerCondition.decode(
                Arrays.copyOfRange(controller.handleRequest(request, funcParam(MapleFunctionCode.CONTROLLER)), 8, 16), 0);
        assertEquals(ControllerCondition.neutral(), first);

        ControllerCondition startPressed = ControllerCondition.neutral().withButton(ControllerButton.START, true);
        controller.setCondition(startPressed);

        ControllerCondition second = ControllerCondition.decode(
                Arrays.copyOfRange(controller.handleRequest(request, funcParam(MapleFunctionCode.CONTROLLER)), 8, 16), 0);
        assertEquals(startPressed, second);
    }
}
