package com.kaleyra.socket_io.engineio.client.transports;


import com.kaleyra.socket_io.engineio.parser.Packet;
import com.kaleyra.socket_io.emitter.Emitter;
import com.kaleyra.socket_io.engineio.client.Transport;
import com.kaleyra.socket_io.engineio.parser.Parser;
import com.kaleyra.socket_io.parseqs.ParseQS;
import com.kaleyra.socket_io.thread.EventThread;
import com.kaleyra.socket_io.yeast.Yeast;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class Polling extends Transport {

    private static final Logger logger = Logger.getLogger(Polling.class.getName());

    public static final String NAME = "polling";

    public static final String EVENT_POLL = "poll";
    public static final String EVENT_POLL_COMPLETE = "pollComplete";

    private boolean polling;


    public Polling(Options opts) {
        super(opts);
        this.name = NAME;
    }

    protected void doOpen() {
        this.poll();
    }

    public void pause(final Runnable onPause) {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                final Polling self = Polling.this;

                Polling.this.readyState = ReadyState.PAUSED;

                final Runnable pause = new Runnable() {
                    @Override
                    public void run() {
                        logger.fine("paused");
                        self.readyState = ReadyState.PAUSED;
                        onPause.run();
                    }
                };

                if (Polling.this.polling || !Polling.this.writable) {
                    final int[] total = new int[]{0};

                    if (Polling.this.polling) {
                        logger.fine("we are currently polling - waiting to pause");
                        total[0]++;
                        Polling.this.once(EVENT_POLL_COMPLETE, new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                logger.fine("pre-pause polling complete");
                                if (--total[0] == 0) {
                                    pause.run();
                                }
                            }
                        });
                    }

                    if (!Polling.this.writable) {
                        logger.fine("we are currently writing - waiting to pause");
                        total[0]++;
                        Polling.this.once(EVENT_DRAIN, new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                logger.fine("pre-pause writing complete");
                                if (--total[0] == 0) {
                                    pause.run();
                                }
                            }
                        });
                    }
                } else {
                    pause.run();
                }
            }
        });
    }

    private void poll() {
        logger.fine("polling");
        this.polling = true;
        this.doPoll();
        this.emit(EVENT_POLL);
    }

    @Override
    protected void onData(String data) {
        _onData(data);
    }

    @Override
    protected void onData(byte[] data) {
        _onData(data);
    }

    private void _onData(Object data) {
        final Polling self = this;
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("polling got data %s", data));
        }
        Parser.DecodePayloadCallback callback = new Parser.DecodePayloadCallback() {
            @Override
            public boolean call(Packet packet, int index, int total) {
                if (self.readyState == ReadyState.OPENING && Packet.OPEN.equals(packet.type)) {
                    self.onOpen();
                }

                if (Packet.CLOSE.equals(packet.type)) {
                    self.onClose();
                    return false;
                }

                self.onPacket(packet);
                return true;
            }
        };

        Parser.decodePayload((String) data, callback);

        if (this.readyState != ReadyState.CLOSED) {
            this.polling = false;
            this.emit(EVENT_POLL_COMPLETE);

            if (this.readyState == ReadyState.OPEN) {
                this.poll();
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("ignoring poll - transport state '%s'", this.readyState));
                }
            }
        }
    }

    protected void doClose() {
        final Polling self = this;

        Emitter.Listener close = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                logger.fine("writing close packet");
                self.write(new Packet[]{new Packet(Packet.CLOSE)});
            }
        };

        if (this.readyState == ReadyState.OPEN) {
            logger.fine("transport open - closing");
            close.call();
        } else {
            // in case we're trying to close while
            // handshaking is in progress (engine.io-client GH-164)
            logger.fine("transport not open - deferring close");
            this.once(EVENT_OPEN, close);
        }
    }

    protected void write(Packet[] packets) {
        final Polling self = this;
        this.writable = false;
        final Runnable callbackfn = new Runnable() {
            @Override
            public void run() {
                self.writable = true;
                self.emit(EVENT_DRAIN);
            }
        };

        Parser.encodePayload(packets, new Parser.EncodeCallback<String>() {
            @Override
            public void call(String data) {
                self.doWrite(data, callbackfn);
            }
        });
    }

    protected String uri() {
        Map<String, String> query = this.query;
        if (query == null) {
            query = new HashMap<String, String>();
        }
        String schema = this.secure ? "https" : "http";
        String port = "";

        if (this.timestampRequests) {
            query.put(this.timestampParam, Yeast.yeast());
        }

        String derivedQuery = ParseQS.encode(query);

        if (this.port > 0 && (("https".equals(schema) && this.port != 443)
                || ("http".equals(schema) && this.port != 80))) {
            port = ":" + this.port;
        }

        if (derivedQuery.length() > 0) {
            derivedQuery = "?" + derivedQuery;
        }

        boolean ipv6 = this.hostname.contains(":");
        return schema + "://" + (ipv6 ? "[" + this.hostname + "]" : this.hostname) + port + this.path + derivedQuery;
    }

    abstract protected void doWrite(String data, Runnable fn);

    abstract protected void doPoll();
}
