package com.kaleyra.socket_io.engineio.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ParserTest {

    static final String ERROR_DATA = "parser error";

    @Test
    public void encodeAsString() {
        Parser.encodePacket(new Packet<String>(Packet.MESSAGE, "test"), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                assertThat(data, isA(String.class));
            }
        });
    }

    @Test
    public void decodeAsPacket()  {
        Parser.encodePacket(new Packet<String>(Packet.MESSAGE, "test"), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                assertThat(Parser.decodePacket(data), isA(Packet.class));
            }
        });
    }

    @Test
    public void noData()  {
        Parser.encodePacket(new Packet(Packet.MESSAGE), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Packet p = Parser.decodePacket(data);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is(nullValue()));
            }
        });
    }

    @Test
    public void encodeOpenPacket()  {
        Parser.encodePacket(new Packet<String>(Packet.OPEN, "{\"some\":\"json\"}"), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Packet<String> p = Parser.decodePacket(data);
                assertThat(p.type, is(Packet.OPEN));
                assertThat(p.data, is("{\"some\":\"json\"}"));
            }
        });
    }

    @Test
    public void encodeClosePacket()  {
        Parser.encodePacket(new Packet<String>(Packet.CLOSE), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Packet p = Parser.decodePacket(data);
                assertThat(p.type, is(Packet.CLOSE));
            }
        });
    }

    @Test
    public void encodePingPacket()  {
        Parser.encodePacket(new Packet<String>(Packet.PING, "1"), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Packet<String> p = Parser.decodePacket(data);
                assertThat(p.type, is(Packet.PING));
                assertThat(p.data, is("1"));
            }
        });
    }

    @Test
    public void encodePongPacket()  {
        Parser.encodePacket(new Packet<String>(Packet.PONG, "1"), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Packet<String> p = Parser.decodePacket(data);
                assertThat(p.type, is(Packet.PONG));
                assertThat(p.data, is("1"));
            }
        });
    }

    @Test
    public void encodeMessagePacket()  {
        Parser.encodePacket(new Packet<String>(Packet.MESSAGE, "aaa"), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Packet<String> p = Parser.decodePacket(data);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is("aaa"));
            }
        });
    }

    @Test
    public void encodeUTF8SpecialCharsMessagePacket()  {
        Parser.encodePacket(new Packet<String>(Packet.MESSAGE, "utf8 — string"), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Packet<String> p = Parser.decodePacket(data);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is("utf8 — string"));
            }
        });
    }

    @Test
    public void encodeMessagePacketCoercingToString()  {
        Parser.encodePacket(new Packet<Integer>(Packet.MESSAGE, 1), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Packet<String> p = Parser.decodePacket(data);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is("1"));
            }
        });
    }

    @Test
    public void encodeUpgradePacket()  {
        Parser.encodePacket(new Packet<String>(Packet.UPGRADE), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Packet p = Parser.decodePacket(data);
                assertThat(p.type, is(Packet.UPGRADE));
            }
        });
    }

    @Test
    public void encodingFormat()  {
        Parser.encodePacket(new Packet<String>(Packet.MESSAGE, "test"), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                assertThat(data.matches("[0-9].*"), is(true));
            }
        });
        Parser.encodePacket(new Packet<String>(Packet.MESSAGE), new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                assertThat(data.matches("[0-9]"), is(true));
            }
        });
    }

    @Test
    public void decodeEmptyPayload() {
        Packet<String> p = Parser.decodePacket((String)null);
        assertThat(p.type, is(Packet.ERROR));
        assertThat(p.data, is(ERROR_DATA));
    }

    @Test
    public void decodeBadFormat() {
        Packet<String> p = Parser.decodePacket(":::");
        assertThat(p.type, is(Packet.ERROR));
        assertThat(p.data, is(ERROR_DATA));
    }

    @Test
    public void decodeInexistentTypes() {
        Packet<String> p = Parser.decodePacket("94103");
        assertThat(p.type, is(Packet.ERROR));
        assertThat(p.data, is(ERROR_DATA));
    }

    @Test
    public void encodePayloads()  {
        Parser.encodePayload(new Packet[]{new Packet(Packet.PING), new Packet(Packet.PONG)}, new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                assertThat(data, isA(String.class));
            }
        });
    }

    @Test
    public void encodeAndDecodePayloads()  {
        Parser.encodePayload(new Packet[] {new Packet<String>(Packet.MESSAGE, "a")}, new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Parser.decodePayload(data, new Parser.DecodePayloadCallback<String>() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        boolean isLast = index + 1 == total;
                        assertThat(isLast, is(true));
                        return true;
                    }
                });
            }
        });
        Parser.encodePayload(new Packet[]{new Packet<String>(Packet.MESSAGE, "a"), new Packet(Packet.PING)}, new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Parser.decodePayload(data, new Parser.DecodePayloadCallback<String>() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        boolean isLast = index + 1 == total;
                        if (!isLast) {
                            assertThat(packet.type, is(Packet.MESSAGE));
                        } else {
                            assertThat(packet.type, is(Packet.PING));
                        }
                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void encodeAndDecodeEmptyPayloads()  {
        Parser.encodePayload(new Packet[] {}, new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Parser.decodePayload(data, new Parser.DecodePayloadCallback<String>() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        assertThat(packet.type, is(Packet.OPEN));
                        boolean isLast = index + 1 == total;
                        assertThat(isLast, is(true));
                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void notUTF8EncodeWhenDealingWithStringsOnly()  {
        Parser.encodePayload(new Packet[] {
                new Packet(Packet.MESSAGE, "€€€"),
                new Packet(Packet.MESSAGE, "α")
        }, new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                assertThat(data, is("4€€€\u001e4α"));
            }
        });
    }

    @Test
    public void decodePayloadBadFormat() {
        Parser.decodePayload("", new Parser.DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
        Parser.decodePayload("))", new Parser.DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
    }

    @Test
    public void decodePayloadBadPacketFormat() {
        Parser.decodePayload("99:", new Parser.DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
        Parser.decodePayload("aa", new Parser.DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
    }

    @Test
    public void encodeBinaryMessage()  {
        final byte[] data = new byte[5];
        for (int i = 0; i < data.length; i++) {
            data[0] = (byte)i;
        }
        Parser.encodePacket(new Packet<byte[]>(Packet.MESSAGE, data), new Parser.EncodeCallback<byte[]>() {
            @Override
            public void call(byte[] encoded) {
                Packet<byte[]> p = Parser.decodePacket(encoded);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is(data));
            }
        });
    }

    @Test
    public void encodeBinaryContents()  {
        final byte[] firstBuffer = new byte[5];
        for (int i = 0 ; i < firstBuffer.length; i++) {
            firstBuffer[0] = (byte)i;
        }
        final byte[] secondBuffer = new byte[4];
        for (int i = 0 ; i < secondBuffer.length; i++) {
            secondBuffer[0] = (byte)(firstBuffer.length + i);
        }

        Parser.encodePayload(new Packet[]{
            new Packet<byte[]>(Packet.MESSAGE, firstBuffer),
            new Packet<byte[]>(Packet.MESSAGE, secondBuffer),
        }, new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                Parser.decodePayload(data, new Parser.DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        boolean isLast = index + 1 == total;
                        assertThat(packet.type, is(Packet.MESSAGE));
                        if (!isLast) {
                            assertThat((byte[])packet.data, is(firstBuffer));
                        } else {
                            assertThat((byte[])packet.data, is(secondBuffer));
                        }
                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void encodeMixedBinaryAndStringContents()  {
        final byte[] firstBuffer = new byte[123];
        for (int i = 0 ; i < firstBuffer.length; i++) {
            firstBuffer[0] = (byte)i;
        }
        Parser.encodePayload(new Packet[]{
            new Packet<byte[]>(Packet.MESSAGE, firstBuffer),
            new Packet<String>(Packet.MESSAGE, "hello"),
            new Packet<String>(Packet.CLOSE),
        }, new Parser.EncodeCallback<String>() {
            @Override
            public void call(String encoded) {
                Parser.decodePayload(encoded, new Parser.DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        if (index == 0) {
                            assertThat(packet.type, is(Packet.MESSAGE));
                            assertThat((byte[])packet.data, is(firstBuffer));
                        } else if (index == 1) {
                            assertThat(packet.type, is(Packet.MESSAGE));
                            assertThat((String)packet.data, is("hello"));
                        } else {
                            assertThat(packet.type, is(Packet.CLOSE));
                        }
                        return true;
                    }
                });
            }
        });
    }
}
