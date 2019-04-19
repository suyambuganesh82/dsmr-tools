InfluxDB

1) Stream splitter
2) DSMR Parser
3) ReplaceText
- Evaluation Mode: Entire text
- Replacement strategy: Always replace
- Replacement value: electricity,equipmentId=${dsmr.equipmentId},p1Version=${dsmr.p1Version} electricityReceivedLowTariff=${dsmr.electricityReceivedLowTariff},electricityReceivedNormalTariff=${dsmr.electricityReceivedNormalTariff},electricityReturnedLowTariff=${dsmr.electricityReturnedLowTariff},electricityReturnedNormalTariff=${dsmr.electricityReturnedNormalTariff},electricityTariffIndicator=${dsmr.electricityTariffIndicator},electricityPowerReceived=${dsmr.electricityPowerReceived},electricityPowerReturned=${dsmr.electricityPowerReturned},powerFailures=${dsmr.powerFailures}i,longPowerFailures=${dsmr.longPowerFailures}i,voltageSagsPhaseL1=${dsmr.voltageSagsPhaseL1}i,voltageSagsPhaseL2=${dsmr.voltageSagsPhaseL2}i,voltageSagsPhaseL3=${dsmr.voltageSagsPhaseL3}i,voltageSwellsPhaseL1=${dsmr.voltageSwellsPhaseL1}i,voltageSwellsPhaseL2=${dsmr.voltageSwellsPhaseL2}i,voltageSwellsPhaseL3=${dsmr.voltageSwellsPhaseL3}i,voltageL1=${dsmr.voltageL1},voltageL2=${dsmr.voltageL2},voltageL3=${dsmr.voltageL3},currentL1=${dsmr.currentL1},currentL2=${dsmr.currentL2},currentL3=${dsmr.currentL3},powerReceivedL1=${dsmr.powerReceivedL1},powerReceivedL2=${dsmr.powerReceivedL2},powerReceivedL3=${dsmr.powerReceivedL3},powerReturnedL1=${dsmr.powerReturnedL1},powerReturnedL2=${dsmr.powerReturnedL2},powerReturnedL3=${dsmr.powerReturnedL3} ${dsmr.timestamp.epochSecond}000000000

4) PutInfluxDB


Take Minifi 0.5.0
Take Nifi 1.7.0  (important use THAT version!!)

nifi-influxdb-nar-1.7.0.nar
nifi-standard-services-api-nar-1.7.0.nar

nifi-sensor-stream-cutter-nar-0.1-SNAPSHOT.nar
nifi-dsmr-parser-nar-0.1-SNAPSHOT.nar


in /etc/fstab

tmpfs /minifi tmpfs nodev,nosuid,size=10M 0 0      



nifi.properties is generated from config.yml!
NoOpProvenanceRepository  --> IN the config.yml  !!!

#nifi.provenance.repository.implementation=org.apache.nifi.provenance.MiNiFiPersistentProvenanceRepository
nifi.provenance.repository.implementation=org.apache.nifi.provenance.NoOpProvenanceRepository
nifi.provenance.repository.rollover.time=1 min

# Volatile Provenance Respository Properties
nifi.provenance.repository.buffer.size=1000


nifi.provenance.repository.buffer.size=1000


FIRST: Run minifi from the ORIGINAL location. Many of the nar files will be unpacked (only once) and the ramdisk is by far not big enough for that.
Once it is running stop it again.


    #/bin/bash
    DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
    RUNDIR=/minifi
    
    RUNDIR=/home/nbasjes/workspace/Testing/TEST/run
    
    if [ ! -e ${RUNDIR}/bin/minifi.sh ];
    then
        mkdir ${RUNDIR}/bin
        cp ${DIR}/bin/*.sh ${RUNDIR}/bin
        chmod 755 ${RUNDIR}/bin/*.sh
        cp -rs ${DIR}/lib   ${RUNDIR}
        cp -rs ${DIR}/conf  ${RUNDIR}
        mkdir ${DIR}/work
        ln -s ${DIR}/work   ${RUNDIR}
    fi
    
    ${RUNDIR}/bin/minifi.sh start
