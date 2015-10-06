package org.jvirtanen.parity.sim;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import org.jvirtanen.nassau.MessageListener;
import org.jvirtanen.nassau.moldudp64.MoldUDP64Client;
import org.jvirtanen.nassau.moldudp64.MoldUDP64ClientState;
import org.jvirtanen.nassau.moldudp64.MoldUDP64ClientStatusListener;
import org.jvirtanen.parity.net.pmd.PMDParser;
import org.jvirtanen.parity.top.Market;
import org.jvirtanen.parity.top.MarketListener;
import org.jvirtanen.parity.top.Side;

class MarketData {

    private MoldUDP64Client transport;

    private TopOfBook topOfBook;

    private MarketData(DatagramChannel channel, DatagramChannel requestChannel,
            InetSocketAddress requestAddress, long instrument) {
        topOfBook = new TopOfBook();

        Market market = new Market(new MarketListener() {

            @Override
            public void bbo(long instrument, long bidPrice, long bidSize, long askPrice, long askSize) {
                topOfBook.bidPrice = bidPrice;
                topOfBook.askPrice = askPrice;
            }

            @Override
            public void trade(long instrument, Side side, long price, long size) {
            }

        });

        market.open(instrument);

        MessageListener listener = new PMDParser(new MarketDataProcessor(market));

        MoldUDP64ClientStatusListener statusListener = new MoldUDP64ClientStatusListener() {

            @Override
            public void state(MoldUDP64ClientState next) {
            }

            @Override
            public void downstream() {
            }

            @Override
            public void request(long sequenceNumber, int requestedMessageCount) {
            }

            @Override
            public void endOfSession() {
            }

        };

        transport = new MoldUDP64Client(channel, requestChannel, requestAddress, listener,
                statusListener);
    }

    public static MarketData open(NetworkInterface multicastInterface,
            InetSocketAddress multicastGroup, InetSocketAddress requestAddress,
            long instrument) throws IOException {
        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);

        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.bind(new InetSocketAddress(multicastGroup.getPort()));
        channel.join(multicastGroup.getAddress(), multicastInterface);
        channel.configureBlocking(false);

        DatagramChannel requestChannel = DatagramChannel.open(StandardProtocolFamily.INET);

        requestChannel.configureBlocking(false);

        return new MarketData(channel, requestChannel, requestAddress, instrument);
    }

    public MoldUDP64Client getTransport() {
        return transport;
    }

    public TopOfBook getTopOfBook() {
        return topOfBook;
    }

    public class TopOfBook {

        private long bidPrice;
        private long askPrice;

        public long getBidPrice() {
            return bidPrice;
        }

        public long getAskPrice() {
            return askPrice;
        }

    }

}
