/*
 * Dutch Smart Meter Requirements (DSMR) Toolkit
 * Copyright (C) 2019-2021 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.basjes.dsmr;

import nl.basjes.dsmr.DSMRTelegram.PowerFailureEvent;
import nl.basjes.dsmr.parse.DsmrBaseVisitor;
import nl.basjes.dsmr.parse.DsmrLexer;
import nl.basjes.dsmr.parse.DsmrParser;
import nl.basjes.dsmr.parse.DsmrParser.CurrentL1Context;
import nl.basjes.dsmr.parse.DsmrParser.CurrentL2Context;
import nl.basjes.dsmr.parse.DsmrParser.CurrentL3Context;
import nl.basjes.dsmr.parse.DsmrParser.ElectricityPowerReceivedContext;
import nl.basjes.dsmr.parse.DsmrParser.ElectricityPowerReturnedContext;
import nl.basjes.dsmr.parse.DsmrParser.ElectricityReceivedLowTariffContext;
import nl.basjes.dsmr.parse.DsmrParser.ElectricityReceivedNormalTariffContext;
import nl.basjes.dsmr.parse.DsmrParser.ElectricityReturnedLowTariffContext;
import nl.basjes.dsmr.parse.DsmrParser.ElectricityReturnedNormalTariffContext;
import nl.basjes.dsmr.parse.DsmrParser.ElectricityTariffIndicatorContext;
import nl.basjes.dsmr.parse.DsmrParser.EquipmentIdContext;
import nl.basjes.dsmr.parse.DsmrParser.LongPowerFailuresContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus1EquipmentIdContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus1ProfileGenericContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus1TypeContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus1UsageContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus2EquipmentIdContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus2ProfileGenericContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus2TypeContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus2UsageContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus3EquipmentIdContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus3ProfileGenericContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus3TypeContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus3UsageContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus4EquipmentIdContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus4ProfileGenericContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus4TypeContext;
import nl.basjes.dsmr.parse.DsmrParser.MBus4UsageContext;
import nl.basjes.dsmr.parse.DsmrParser.MessageCodesContext;
import nl.basjes.dsmr.parse.DsmrParser.MessageContext;
import nl.basjes.dsmr.parse.DsmrParser.P1VersionContext;
import nl.basjes.dsmr.parse.DsmrParser.PowerFailureEventLogContext;
import nl.basjes.dsmr.parse.DsmrParser.PowerFailuresContext;
import nl.basjes.dsmr.parse.DsmrParser.PowerReceivedL1Context;
import nl.basjes.dsmr.parse.DsmrParser.PowerReceivedL2Context;
import nl.basjes.dsmr.parse.DsmrParser.PowerReceivedL3Context;
import nl.basjes.dsmr.parse.DsmrParser.PowerReturnedL1Context;
import nl.basjes.dsmr.parse.DsmrParser.PowerReturnedL2Context;
import nl.basjes.dsmr.parse.DsmrParser.PowerReturnedL3Context;
import nl.basjes.dsmr.parse.DsmrParser.TelegramContext;
import nl.basjes.dsmr.parse.DsmrParser.TimestampContext;
import nl.basjes.dsmr.parse.DsmrParser.VoltageL1Context;
import nl.basjes.dsmr.parse.DsmrParser.VoltageL2Context;
import nl.basjes.dsmr.parse.DsmrParser.VoltageL3Context;
import nl.basjes.dsmr.parse.DsmrParser.VoltageSagsPhaseL1Context;
import nl.basjes.dsmr.parse.DsmrParser.VoltageSagsPhaseL2Context;
import nl.basjes.dsmr.parse.DsmrParser.VoltageSagsPhaseL3Context;
import nl.basjes.dsmr.parse.DsmrParser.VoltageSwellsPhaseL1Context;
import nl.basjes.dsmr.parse.DsmrParser.VoltageSwellsPhaseL2Context;
import nl.basjes.dsmr.parse.DsmrParser.VoltageSwellsPhaseL3Context;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

// CHECKSTYLE.OFF: LineLength
// CHECKSTYLE.OFF: LeftCurly
// CHECKSTYLE.OFF: ParamPad
// CHECKSTYLE.OFF: MethodParamPad
public final class ParseDsmrTelegram extends DsmrBaseVisitor<Void> implements ANTLRErrorListener {

    private boolean hasSyntaxError = false;

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object o, int i, int i1, String s, RecognitionException e) {
        hasSyntaxError = true;
        dsmrTelegram.isValid = false;
    }

    @Override
    public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet, ATNConfigSet atnConfigSet) {
        // Ignore this type of problem
    }

    @Override
    public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet, ATNConfigSet atnConfigSet) {
        // Ignore this type of problem
    }

    @Override
    public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet) {
        // Ignore this type of problem
    }

    public static synchronized DSMRTelegram parse(String telegram) {
        return new ParseDsmrTelegram(telegram).parse();
    }

    private final String          telegramString;
    private final DSMRTelegram    dsmrTelegram;
    private final TimestampParser timestampParser = new TimestampParser();

    private ParseDsmrTelegram(String telegram) {
        telegramString = telegram;
        dsmrTelegram = new DSMRTelegram();
        dsmrTelegram.validCRC = CheckCRC.crcIsValid(telegramString);
        dsmrTelegram.isValid = dsmrTelegram.validCRC;
    }

    private DSMRTelegram parse() {
        if (telegramString == null || telegramString.isEmpty()) {
            dsmrTelegram.isValid = false;
            return null;
        }

        CodePointCharStream input = CharStreams.fromString(telegramString);
        DsmrLexer           lexer = new DsmrLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        DsmrParser parser = new DsmrParser(tokens);

        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(this);
        parser.addErrorListener(this);

        TelegramContext telegramContext = parser.telegram();

        if (telegramContext.ident == null) {
            return dsmrTelegram; // Unparsable
        }

        this.visitTelegram(telegramContext);

        // Final step cross map the MBus events into usable attributes.
        fillMBusDataToAttributes(dsmrTelegram);

        // Really old records do not have a P1 version AND do not have a CRC.
        // which makes these records valid.
        if (hasSyntaxError) {
            dsmrTelegram.isValid = false;
        } else {
            if ((dsmrTelegram.crc == null || dsmrTelegram.crc.isEmpty()) &&
                (dsmrTelegram.p1Version == null || dsmrTelegram.p1Version.isEmpty())) {
                dsmrTelegram.isValid = true;
            } else {
                dsmrTelegram.isValid = dsmrTelegram.validCRC;
            }
        }

        // Sanitize the p1Version
        if (dsmrTelegram.p1Version == null || dsmrTelegram.p1Version.isEmpty()) {
            dsmrTelegram.p1Version = "2.2";
        } else {
            dsmrTelegram.p1Version = dsmrTelegram.p1Version.replaceAll("([0-9])([0-9]+)", "$1.$2");
        }

        return dsmrTelegram;
    }

    private void mustHaveUnit(MBusEvent mBusEvent, String unit) {
        if (mBusEvent.unit != null && !mBusEvent.unit.isEmpty() && !unit.equals(mBusEvent.unit)) {
            hasSyntaxError = true;
        }
    }

    private void fillMBusDataToAttributes(DSMRTelegram telegram) {
        for (Map.Entry<Integer, MBusEvent> mBusEventEntry: telegram.mBusEvents.entrySet()) {
            MBusEvent mBusEvent = mBusEventEntry.getValue();

            // This mapping is based on the documentation found on http://www.m-bus.com/
            switch (mBusEvent.deviceType) {
//                   0x00: // Other                                                                 0000 0000  00
//                   0x01: // Oil                                                                   0000 0001  01
                case 0x02: // Electricity via a slave                                               0000 0010  02
                    if (telegram.slaveEMeterEquipmentId == null) {
                        telegram.slaveEMeterEquipmentId         = mBusEvent.equipmentId;
                        telegram.slaveEMeterTimestamp           = mBusEvent.timestamp;
                        telegram.slaveEMeterkWh                 = mBusEvent.value;
                        mustHaveUnit(mBusEvent, "kWh");
                    }
                    break;

                case 0x03: // Gas                                                                   0000 0011  03
                    if (telegram.gasEquipmentId== null) {
                        telegram.gasEquipmentId                 = mBusEvent.equipmentId;
                        telegram.gasTimestamp                   = mBusEvent.timestamp;
                        telegram.gasM3                          = mBusEvent.value;
                        mustHaveUnit(mBusEvent, "m3");
                    }
                    break;

//                   0x04: // Heat (Volume measured at return temperature: outlet)                  0000 0100  04
//                   0x05: // Steam                                                                 0000 0101  05
//                   0x06: // Warm Water (30-90 Celcius)                                            0000 0110  06
//                   0x07: // Water                                                                 0000 0111  07
//                   0x08: // Heat Cost Allocator                                                   0000 1000  08
//                   0x09: // Compressed Air                                                        0000 1001  09
//                   0x0A: // Cooling load meter (Volume measured at return temperature: outlet)    0000 1010  0A
//                   0x0B: // Cooling load meter (Volume measured at flow temperature: inlet)       0000 1011  0B
//                   0x0C: // Heat (Volume measured at flow temperature: inlet)                     0000 1100  0C
//                   0xOD: // Heat / Cooling load meter                                             0000 1101  OD
//                   0x0E: // Bus / System                                                          0000 1110  0E
//                   0x0F: // Unknown Medium                                                        0000 1111  0F
//                   0x10: // Reserve                                                               .......... 10 - 14
//                   0x15: // Hot Water (>90 Celsius)                                               0001 0101  15
//                   0x16: // Cold Water                                                            0001 0110  16
//                   0x17: // Dual register (hot/cold) Water                                        0001 0111  17
//                   0x18: // Pressure                                                              0001 1000  18
//                   0x19: // A/D Converter                                                         0001 1001  19
//                   0x20: // Reserve                                                               .......... 20 - FF
                default: // We simply do not map the ones we do not understand
            }
        }
    }

    // https://stackoverflow.com/questions/50712987/hex-string-to-byte-array-conversion-java
    private byte[] hexStringToByteArray(String s) {
        byte[] data = new byte[s.length()/2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ((Character.digit(s.charAt(i*2), 16) << 4)
                + Character.digit(s.charAt(i*2 + 1), 16));
        }
        return data;
    }

    private String hexStringToString(String hexString) {
        return new String(hexStringToByteArray(hexString), UTF_8);
    }

    private static final Pattern IDENT_PATTERN = Pattern.compile("^/([a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9])5(.*)$");

    @Override
    public Void visitTelegram(TelegramContext ctx) {
        dsmrTelegram.rawIdent = ctx.ident.getText();
        Matcher identMatcher = IDENT_PATTERN.matcher(dsmrTelegram.rawIdent);
        if (identMatcher.find()) {
            dsmrTelegram.equipmentBrandTag = identMatcher.group(1).toUpperCase(Locale.ROOT);
            dsmrTelegram.ident = identMatcher.group(2);

            // Some brands have a very unclean identification string.
            if (dsmrTelegram.ident.startsWith("\\2")) {
                dsmrTelegram.ident = dsmrTelegram.ident.substring(2);
            } else {
                if (dsmrTelegram.ident.startsWith("\\")) {
                    dsmrTelegram.ident = dsmrTelegram.ident.substring(1);
                }
            }
            dsmrTelegram.ident = dsmrTelegram.ident.trim();
        } else {
            // If it does not match the expected pattern just use the entire thing.
            dsmrTelegram.ident = dsmrTelegram.rawIdent;
        }
        if (ctx.crc == null || ctx.crc.getText().isEmpty()) {
            dsmrTelegram.crc = null;
        } else {
            dsmrTelegram.crc = ctx.crc.getText().substring(1); // Skip the '!' at the start
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitP1Version   (P1VersionContext   ctx) {
        dsmrTelegram.p1Version   = ctx.version.getText();
        return null;
    }

    @Override
    public Void visitTimestamp   (TimestampContext   ctx) {
        dsmrTelegram.timestamp   = timestampParser.parse(ctx.timestamp.getText());
        return null;
    }

    @Override
    public Void visitEquipmentId (EquipmentIdContext ctx) {
        dsmrTelegram.equipmentId = hexStringToString(ctx.id.getText()).trim();
        return null;
    }

    @Override
    public Void visitMessageCodes(MessageCodesContext ctx) {
        // Text message codes: numeric 8 digits.
        dsmrTelegram.messageCodes = (ctx.text == null) ? "" : hexStringToString(ctx.text.getText());
        return null;
    }

    @Override
    public Void visitMessage     (MessageContext     ctx) {
        // Text message max 1024 characters.
        dsmrTelegram.message     = (ctx.text == null) ? "" : hexStringToString(ctx.text.getText());
        return null;
    }

    @Override
    public Void visitPowerFailureEventLog(PowerFailureEventLogContext ctx) {
        dsmrTelegram.powerFailureEventLogSize =  Long.valueOf(ctx.count.getText());
        dsmrTelegram.powerFailureEventLog = new ArrayList<>();
        visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitPowerFailureEvent(DsmrParser.PowerFailureEventContext ctx) {
        PowerFailureEvent powerFailureEvent = new PowerFailureEvent();

        // The provided timestamp is the end of the failure
        powerFailureEvent.endTime = timestampParser.parse(ctx.eventTime.getText());

        // The parser ONLY allows a eventDurationUnit of seconds because the specification explicitly states that.
        powerFailureEvent.duration =  Duration.ofSeconds(Long.parseLong(ctx.eventDuration.getText()));

        // For convenience we calculate the start time
        powerFailureEvent.startTime = powerFailureEvent.endTime.minus(powerFailureEvent.duration);

        dsmrTelegram.powerFailureEventLog.add(powerFailureEvent);
        return null;
    }

    @Override public Void visitElectricityTariffIndicator       (ElectricityTariffIndicatorContext       ctx) { dsmrTelegram.electricityTariffIndicator      = Long.valueOf(ctx.value.getText()); return null; } // Tariff indicator electricity
    @Override public Void visitElectricityReceivedLowTariff     (ElectricityReceivedLowTariffContext     ctx) { dsmrTelegram.electricityReceivedLowTariff    = Double.valueOf(ctx.value.getText()); return null; } // Meter Reading electricity delivered to client (low tariff) in 0,001 kWh
    @Override public Void visitElectricityReceivedNormalTariff  (ElectricityReceivedNormalTariffContext  ctx) { dsmrTelegram.electricityReceivedNormalTariff = Double.valueOf(ctx.value.getText()); return null; } // Meter Reading electricity delivered to client (normal tariff) in 0,001 kWh
    @Override public Void visitElectricityReturnedLowTariff     (ElectricityReturnedLowTariffContext     ctx) { dsmrTelegram.electricityReturnedLowTariff    = Double.valueOf(ctx.value.getText()); return null; } // Meter Reading electricity delivered by client (low tariff) in 0,001 kWh
    @Override public Void visitElectricityReturnedNormalTariff  (ElectricityReturnedNormalTariffContext  ctx) { dsmrTelegram.electricityReturnedNormalTariff = Double.valueOf(ctx.value.getText()); return null; } // Meter Reading electricity delivered by client (normal tariff) in 0,001 kWh
    @Override public Void visitElectricityPowerReceived         (ElectricityPowerReceivedContext         ctx) { dsmrTelegram.electricityPowerReceived        = Double.valueOf(ctx.value.getText()); return null; } // Actual electricity power delivered (+P) in 1 Watt resolution
    @Override public Void visitElectricityPowerReturned         (ElectricityPowerReturnedContext         ctx) { dsmrTelegram.electricityPowerReturned        = Double.valueOf(ctx.value.getText()); return null; } // Actual electricity power received (-P) in 1 Watt resolution

    @Override public Void visitPowerFailures                    (PowerFailuresContext                    ctx) { dsmrTelegram.powerFailures                   = Long.valueOf(ctx.count.getText());   return null; } // Number of power failures in any phases
    @Override public Void visitLongPowerFailures                (LongPowerFailuresContext                ctx) { dsmrTelegram.longPowerFailures               = Long.valueOf(ctx.count.getText());   return null; } // Number of long power failures in any phases

    @Override public Void visitVoltageSagsPhaseL1               (VoltageSagsPhaseL1Context               ctx) { dsmrTelegram.voltageSagsPhaseL1              = Long.valueOf(ctx.count.getText());   return null; } // Number of voltage sags in phase L1
    @Override public Void visitVoltageSagsPhaseL2               (VoltageSagsPhaseL2Context               ctx) { dsmrTelegram.voltageSagsPhaseL2              = Long.valueOf(ctx.count.getText());   return null; } // Number of voltage sags in phase L2
    @Override public Void visitVoltageSagsPhaseL3               (VoltageSagsPhaseL3Context               ctx) { dsmrTelegram.voltageSagsPhaseL3              = Long.valueOf(ctx.count.getText());   return null; } // Number of voltage sags in phase L3
    @Override public Void visitVoltageSwellsPhaseL1             (VoltageSwellsPhaseL1Context             ctx) { dsmrTelegram.voltageSwellsPhaseL1            = Long.valueOf(ctx.count.getText());   return null; } // Number of voltage swells in phase L1
    @Override public Void visitVoltageSwellsPhaseL2             (VoltageSwellsPhaseL2Context             ctx) { dsmrTelegram.voltageSwellsPhaseL2            = Long.valueOf(ctx.count.getText());   return null; } // Number of voltage swells in phase L2
    @Override public Void visitVoltageSwellsPhaseL3             (VoltageSwellsPhaseL3Context             ctx) { dsmrTelegram.voltageSwellsPhaseL3            = Long.valueOf(ctx.count.getText());   return null; } // Number of voltage swells in phase L3
    @Override public Void visitVoltageL1                        (VoltageL1Context                        ctx) { dsmrTelegram.voltageL1                       = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous voltage L1
    @Override public Void visitVoltageL2                        (VoltageL2Context                        ctx) { dsmrTelegram.voltageL2                       = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous voltage L2
    @Override public Void visitVoltageL3                        (VoltageL3Context                        ctx) { dsmrTelegram.voltageL3                       = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous voltage L3
    @Override public Void visitCurrentL1                        (CurrentL1Context                        ctx) { dsmrTelegram.currentL1                       = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous current L1
    @Override public Void visitCurrentL2                        (CurrentL2Context                        ctx) { dsmrTelegram.currentL2                       = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous current L2
    @Override public Void visitCurrentL3                        (CurrentL3Context                        ctx) { dsmrTelegram.currentL3                       = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous current L3
    @Override public Void visitPowerReceivedL1                  (PowerReceivedL1Context                  ctx) { dsmrTelegram.powerReceivedL1                 = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous active power L1 (+P)
    @Override public Void visitPowerReceivedL2                  (PowerReceivedL2Context                  ctx) { dsmrTelegram.powerReceivedL2                 = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous active power L2 (+P)
    @Override public Void visitPowerReceivedL3                  (PowerReceivedL3Context                  ctx) { dsmrTelegram.powerReceivedL3                 = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous active power L3 (+P)
    @Override public Void visitPowerReturnedL1                  (PowerReturnedL1Context                  ctx) { dsmrTelegram.powerReturnedL1                 = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous active power L1 (-P)
    @Override public Void visitPowerReturnedL2                  (PowerReturnedL2Context                  ctx) { dsmrTelegram.powerReturnedL2                 = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous active power L2 (-P)
    @Override public Void visitPowerReturnedL3                  (PowerReturnedL3Context                  ctx) { dsmrTelegram.powerReturnedL3                 = Double.valueOf(ctx.value.getText()); return null; } // Instantaneous active power L3 (-P)

    private MBusEvent getMBusEvent(int index) {
        return dsmrTelegram.mBusEvents.computeIfAbsent(index, i -> new MBusEvent());
    }

    private void setMBusType(int index, int type) {
        MBusEvent mBusEvent = getMBusEvent(index);
        mBusEvent.deviceType = type;
    }

    @Override public Void visitMBus1Type(MBus1TypeContext ctx) { setMBusType(1, Integer.parseInt(ctx.type.getText())); return null; }
    @Override public Void visitMBus2Type(MBus2TypeContext ctx) { setMBusType(2, Integer.parseInt(ctx.type.getText())); return null; }
    @Override public Void visitMBus3Type(MBus3TypeContext ctx) { setMBusType(3, Integer.parseInt(ctx.type.getText())); return null; }
    @Override public Void visitMBus4Type(MBus4TypeContext ctx) { setMBusType(4, Integer.parseInt(ctx.type.getText())); return null; }

    private void setMBusEquipmentId(int index, String equipmentId) {
        MBusEvent mBusEvent = getMBusEvent(index);
        mBusEvent.equipmentId = hexStringToString(equipmentId);
    }

    @Override public Void visitMBus1EquipmentId(MBus1EquipmentIdContext ctx) { setMBusEquipmentId(1, ctx.id.getText()); return null; }
    @Override public Void visitMBus2EquipmentId(MBus2EquipmentIdContext ctx) { setMBusEquipmentId(2, ctx.id.getText()); return null; }
    @Override public Void visitMBus3EquipmentId(MBus3EquipmentIdContext ctx) { setMBusEquipmentId(3, ctx.id.getText()); return null; }
    @Override public Void visitMBus4EquipmentId(MBus4EquipmentIdContext ctx) { setMBusEquipmentId(4, ctx.id.getText()); return null; }

    private void setMBusUsage(int index, Token timestamp, Token value, Token unit) {
        MBusEvent mBusEvent = getMBusEvent(index);
        mBusEvent.timestamp = timestamp == null ? null : timestampParser.parse(timestamp.getText());
        mBusEvent.value     = value     == null ? null : Double.valueOf(value.getText());
        mBusEvent.unit      = unit      == null ? ""   : unit.getText();
    }

    @Override public Void visitMBus1Usage(MBus1UsageContext ctx) { setMBusUsage(1, ctx.timestamp, ctx.value, ctx.unit); return null; }
    @Override public Void visitMBus2Usage(MBus2UsageContext ctx) { setMBusUsage(2, ctx.timestamp, ctx.value, ctx.unit); return null; }
    @Override public Void visitMBus3Usage(MBus3UsageContext ctx) { setMBusUsage(3, ctx.timestamp, ctx.value, ctx.unit); return null; }
    @Override public Void visitMBus4Usage(MBus4UsageContext ctx) { setMBusUsage(4, ctx.timestamp, ctx.value, ctx.unit); return null; }

    @Override public Void visitMBus1ProfileGeneric(MBus1ProfileGenericContext ctx) { setMBusUsage(1, ctx.timestamp, ctx.value, ctx.unit); return null; }
    @Override public Void visitMBus2ProfileGeneric(MBus2ProfileGenericContext ctx) { setMBusUsage(2, ctx.timestamp, ctx.value, ctx.unit); return null; }
    @Override public Void visitMBus3ProfileGeneric(MBus3ProfileGenericContext ctx) { setMBusUsage(3, ctx.timestamp, ctx.value, ctx.unit); return null; }
    @Override public Void visitMBus4ProfileGeneric(MBus4ProfileGenericContext ctx) { setMBusUsage(4, ctx.timestamp, ctx.value, ctx.unit); return null; }

    @Override
    public Void visitUnknownCosemId(DsmrParser.UnknownCosemIdContext ctx) {
        // Ignore
        return null;
    }
}
