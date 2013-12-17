/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.ais.abnormal.analyzer;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dk.dma.ais.abnormal.analyzer.analysis.Analysis;
import dk.dma.ais.abnormal.analyzer.analysis.ShipTypeAndSizeAnalysis;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.IPositionMessage;
import dk.dma.ais.packet.AisPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Set;

/**
 * Handler for read AIS packets
 */
public class PacketHandlerImpl implements PacketHandler {

    static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    
    private AppStatisticsService statisticsService;
    private TrackingService trackingService;

    private Set<Analysis> analyses;

    @Inject
    public PacketHandlerImpl(AppStatisticsService statisticsService, TrackingService trackingService) {
        LOG.info("AIS packet handler created (" + this + ").");
        this.statisticsService = statisticsService;
        this.trackingService = trackingService;
        initAnalyses();
    }

    public void accept(final AisPacket packet) {
        statisticsService.incUnfilteredPacketCount();
        // TODO no packet filtering yet
        statisticsService.incFilteredPacketCount();
        long n = statisticsService.getFilteredPacketCount();
        if (n % 100000L == 0) {
            LOG.debug(n + " packets passed through filter.");
        }

        // Get AisMessage from packet or drop
        AisMessage message = packet.tryGetAisMessage();
        if (message == null) {
            return;
        }
        statisticsService.incMessageCount();

        if (message instanceof IPositionMessage) {
            statisticsService.incPosMsgCount();
        } else if (message instanceof AisMessage5) {
            statisticsService.incStatMsgCount();
        }

        Date timestamp = packet.getTimestamp();

        doWork(timestamp, message);
    }

    private void doWork(Date timestamp, AisMessage message) {
        trackingService.update(timestamp, message);
        statisticsService.setTrackCount(trackingService.getNumberOfTracks());
    }

    private void initAnalyses() {
        Injector injector = AbnormalAnalyzerApp.getInjector();

        this.analyses = new ImmutableSet.Builder<Analysis>()
                .add(injector.getInstance(ShipTypeAndSizeAnalysis.class))
                .build();

        /*
        Iterator<Analysis> analysisIterator = this.analyses.iterator();
        while (analysisIterator.hasNext()) {
            analysisIterator.next().start();
        }
        */
    }

}