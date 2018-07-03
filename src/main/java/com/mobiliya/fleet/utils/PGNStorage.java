package com.mobiliya.fleet.utils;

import android.util.Log;

import com.mobiliya.fleet.models.PGN;
import com.mobiliya.fleet.models.Parameter;

import java.util.ArrayList;
import java.util.List;


public class PGNStorage {
    private static final PGNStorage instance = new PGNStorage();
    private List<PGN> pgnList = new ArrayList<>();

    private Parameter param = new Parameter();

    public Parameter getParameter() {

        return param;
    }

    private PGNStorage() {
        //Red
        pgnList.add(new PGN("64879", "5-6", "0.5", "0", "kPa", "EGR_CoolerInP_kPa"));
        pgnList.add(new PGN("64961", "3", "2", "0", "kPa", "EGR_ValveInP_kPa"));
        pgnList.add(new PGN("64961", "6", "2", "0", "kPa", "EGR_ValveOutP_kPa"));
        pgnList.add(new PGN("64830", "1-2", "0.03125", "-273", "deg C", "SCR_InT_C"));
        pgnList.add(new PGN("64830", "4-5", "0.03125", "-273", "deg C", "SCR_OutT_C"));
        pgnList.add(new PGN("64739", "1", "2", "0", "kPa", "TrboOutP_kPa1"));
        pgnList.add(new PGN("64739", "4", "2", "0", "kPa", "TrboOutP_kPa2"));
        pgnList.add(new PGN("65188", "5-6", "0.0078125", "-250", "kPa", "EGR_DiffP_kPa"));
        pgnList.add(new PGN("64470", "1-2", "0.1", "0", "kPa", "Exh_mfldP_kPa"));
        pgnList.add(new PGN("64908", "1-2", "0.1", "0", "kPa", "DPF_InPress1_kPa"));
        pgnList.add(new PGN("64652", "6-7", "6", "0", "s", "DPF_regenINT_s"));
        pgnList.add(new PGN("64878", "5", "0.4", "0", "%", "SCR_Eff_%"));
        pgnList.add(new PGN("65262", "5-6", "0.03125", "-273", "deg C", "TrbOilT_C"));
        pgnList.add(new PGN("64948", "1-2", "0.03125", "-273", "deg C", "Exh_BeginT_C"));
        pgnList.add(new PGN("64564", "1-2", "5", "0", "kPa", "FuelRailP_kPa"));
        pgnList.add(new PGN("64557", "5", "1", "-40", "deg C", "FuelRailT_C"));
        pgnList.add(new PGN("64709", "1-2", "0.03125", "-273", "deg C", "SCR_MidT_C"));
        pgnList.add(new PGN("64946", "1-2", "0.03125", "-273", "deg C", "Exh_MidT_C"));
        pgnList.add(new PGN("64946", "3-4", "0.03125", "-273", "deg C", "DPF_MidT_C"));
        pgnList.add(new PGN("64947", "1-2", "0.03125", "-273", "deg C", "Exh_EndT_C"));
        pgnList.add(new PGN("64488", "1-4", "0.5", "0", "L", "TotDEF_L"));
        pgnList.add(new PGN("64701", "5-8", "0.05", "0", "L", "TripDEF_L"));
        pgnList.add(new PGN("64891", "2", "1.0", "0", "%", "DPF_Ash_%"));
        //Cyne
        pgnList.add(new PGN("65251", "20-21", "1", "0", "Nm", "RefEngTorq_Nm"));
        pgnList.add(new PGN("64920", "13-16", "1", "0", "count", "TotActRegens_cnt"));
        pgnList.add(new PGN("64920", "21-24", "1", "0", "count", "TotPassRegens_cnt"));
        pgnList.add(new PGN("65200", "9-12", "0.05", "0", "h", "TripTime_h"));
        //Orange
        pgnList.add(new PGN("65262", "2", "1", "-40", "deg C", "EngFuelTemp_C"));
        pgnList.add(new PGN("65270", "5", "0.05", "0", "kPa", "AirFilterDiffPress_kPa"));
        pgnList.add(new PGN("33792", "2-3", "0.03125", "-273", "deg C", "DPF_InTemp_C"));
        pgnList.add(new PGN("64662", "1-2", "000.125", "0", "kPa", "TrboInP_kPa"));
        //Green
        pgnList.add(new PGN("65248", "5-8", "0.125", "0", "km", "TotVehDist_km"));
        pgnList.add(new PGN("65253", "1-4", "0.05", "0", "Hrs", "TotEngTime_hrs"));
        pgnList.add(new PGN("65266", "5-6", "0.001953125", "0", "km/L", "AveFuelEcon_km/L"));
        pgnList.add(new PGN("65248", "1-4", "0.125", "0", "km", "TripDist_km"));
        pgnList.add(new PGN("65257", "1-4", "0.5", "0", "L", "TotFuelUsed_kg"));
        pgnList.add(new PGN("65257", "5-8", "0.5", "0", "L", "TripFuel_kg"));
        pgnList.add(new PGN("65244", "1-4", "0.5", "0", "L", "TotIdleFuel_L"));
        pgnList.add(new PGN("65244", "5-8", "0.05", "0", "Hrs", "TotIdleTime_hrs"));
        pgnList.add(new PGN("65266", "1-2", "0.05", "0", "L/h", "EngFuelRate_L/h"));
        pgnList.add(new PGN("65265", "2-3", "0.00390625", "0", "km/h", "VehicleSpd_km/h"));
        pgnList.add(new PGN("61444", "4-5", "0.125", "0", "rpm", "EngSpd_rpm"));
        pgnList.add(new PGN("61444", "3", "0.125", "-125", "%", "EngTorq_%"));
        pgnList.add(new PGN("65263", "4", "4", "0", "kPa", "EngOilPres_kPa"));
        pgnList.add(new PGN("65262", "3-4", "0.03125", "-273", "degC", "EngOilTemp_C"));
        pgnList.add(new PGN("65262", "1", "1", "-40", "degC", "EngCoolTemp_C"));
        pgnList.add(new PGN("65269", "4-5", "0.03125", "-273", "degC", "AmbAirTemp_C"));
        pgnList.add(new PGN("65270", "2", "2", "0", "kPa", "EngMfldPress_kPa"));
        pgnList.add(new PGN("65270", "3", "1", "-40", "degC", "EngMfldTemp_C"));
        pgnList.add(new PGN("65263", "5-6", "0.0078125", "-250", "kPa", "EngCrnkCaseP_kPa"));
        pgnList.add(new PGN("65188", "7-8", "0.03125", "-273", "degC", "EngEGRTemp_C"));
        pgnList.add(new PGN("64908", "3-4", "0.1", "0", "kPa", "DPF_OutPress1_kPa"));
        pgnList.add(new PGN("61454", "1-2", "0.05", "-200", "ppm", "SCR_InNOx_ppm"));
        pgnList.add(new PGN("61455", "1-2", "0.05", "-200", "ppm", "SCR_OutNOx_ppm"));
        pgnList.add(new PGN("64946", "5-6", "0.1", "0", "kPa", "DPF_PressDiff_kPa"));
        pgnList.add(new PGN("64891", "1", "1.0", "0", "%", "DPF_Soot_%"));
        pgnList.add(new PGN("64947", "3-4", "0.03125", "-273", "degC", "DPF_OutT_C"));
        pgnList.add(new PGN("64948", "3-4", "0.03125", "-273", "degC", "DPF_InT_C"));
        pgnList.add(new PGN("65245", "2-3", "4", "0", "rpm", "TrboSpd_rpm"));
        //Multipackets
        pgnList.add(new PGN("64920", "1-4", "0.5", "0", "l", "AT_Tot_Fuel_L"));
        pgnList.add(new PGN("64920", "5-8", "1", "0", "s", "AT_Tot_Reg_Tm"));
        pgnList.add(new PGN("64920", "13-16", "1", "0", "count", "TotActRegen_cnt"));
        pgnList.add(new PGN("64920", "17-20", "1", "0", "s", "TotPassRegen_Tm"));
        pgnList.add(new PGN("64920", "21-24", "1", "0", "count", "TotPassRegen_cnt"));
        pgnList.add(new PGN("64920", "33-36", "1", "0", "s", "AvgTimeReg_s"));
        pgnList.add(new PGN("64920", "37-40", "0.125", "0", "km", "AvgDistReg_km"));
        pgnList.add(new PGN("65251", "20-21", "1", "0", "Nm", "RefEngTorq_Nm"));
        pgnList.add(new PGN("64920", "9-12", "1", "0", "s", "AT_Dsbld_Tm"));
        pgnList.add(new PGN("64920", "25-28", "1", "0", "count", "AT_Inhibit_Req"));
        pgnList.add(new PGN("64920", "29-32", "1", "0", "count", "AT_Act_Reg_Req"));

        pgnList.add(new PGN("65269", "1", "0.5", "0", "kPa", "Barometric_Pressure"));
        pgnList.add(new PGN("65213", "2", "0.4", "0", "%", "Fan_Drive_State"));

    }

    public static PGNStorage getInstance() {
        return instance;
    }

    private List<PGN> getPGN(String pgn) {

        List<PGN> pgs = new ArrayList<>();
        for (PGN p : pgnList) {
            if (p.getPGN().trim().equalsIgnoreCase(pgn)) {
                pgs.add(p);
            }

        }
        return pgs;
    }

    public List<PGN> calculatePGNValues(String pgn, byte[] values) {

        List<PGN> pgns = getPGN(pgn);
        if (pgns == null || pgns.size() == 0) {
            //Log.d("****&&&" , "Invalid PGN " + pgn);
            return null;
        }
        for (PGN p : pgns) {

            int startByteLocation;
            int stopByteLocation;

            int rawValue = -1;
            byte[] rawShortArray = new byte[2];

            byte[] rawIntArray = new byte[4];

            if (p.getLocation().length() > 2) {
                startByteLocation = Integer.parseInt(p.getLocation().split("-")[0]) - 1;
                stopByteLocation = Integer.parseInt(p.getLocation().split("-")[1]) - 1;

                if (stopByteLocation - startByteLocation == 1) {
                    rawShortArray = new byte[]{values[startByteLocation], values[stopByteLocation]};
                    rawValue = convertByteArrayToShort(rawShortArray);
                } else {
                    rawIntArray = new byte[]{values[startByteLocation], values[startByteLocation + 1], values[startByteLocation + 2], values[stopByteLocation]};
                    rawValue = convertByteArrayToInt(rawIntArray);
                }

            } else {
                rawValue = values[Integer.parseInt(p.getLocation().trim()) - 1];
            }

            if (rawValue > 0) {

                //byte[] actualBytes = integerToByteArray(rawValue);

                double scaleFactor = Double.parseDouble(p.getScaling().trim());

                float offset = Float.parseFloat(p.getOffset().trim());

                String hexString = Double.toHexString(rawValue);
                hexString = hexString.equalsIgnoreCase("String") ? "NA" : hexString;
                p.setHexRawValue(hexString);

                p.setRawValue(rawValue);

                double actualValue = rawValue * scaleFactor + offset;

                p.setActualValues(actualValue);
            }
            Log.d("#####", "PGN: " + p.getPGN() + ", Parameter: " + p.getParameterName() +
                    ", Actual Value: " + p.getActualValues() + ", Raw Value: " + p.getRawValue() +
                    ", Hex Value: " + p.getHexRawValue());
        }
        return pgns;
    }

    private short convertByteArrayToShort(byte[] bytes) {
        int r = bytes[1] & 0xFF;
        r = (r << 8) | (bytes[0] & 0xFF);

        return (short) r;
    }


    private int convertByteArrayToInt(byte[] bytes) {

        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    public Parameter setParameterObject(Parameter param, List<PGN> pgns) {
        for (PGN p : pgns) {
            param.PGN = Integer.parseInt(p.getPGN().trim());
            param.PGNActualValue = String.valueOf(p.getActualValues());
            param.PGNRawValue = String.valueOf(p.getRawValue());
            String HexVal = p.getHexRawValue();
            String pName = p.getParameterName().trim();

            param.PGNParameterName = pName;
            param.PGNHexValue = HexVal;


            switch (pName) {
                case "AmbAirTemp_C":
                    param.AmbientTemp = p.getActualValues();
                    param.AmbientTempRaw = p.getRawValue();
                    param.AmbientTempHex = p.getHexRawValue();
                    break;
                case "DPF_OutT_C":
                    param.DPFOutletTemp = p.getActualValues();
                    param.DPFOutletTempRaw = p.getRawValue();
                    param.DPFOutletTempHex = p.getHexRawValue();
                    break;
                case "EngMfldTemp_C":
                    param.EngineIntakeManifoldTemp = p.getActualValues();
                    param.EngineIntakeManifoldTempRaw = p.getRawValue();
                    param.EngineIntakeManifoldTempHex = p.getHexRawValue();
                    break;
                case "DPF_InT_C":
                    param.DPFInletTemp = p.getActualValues();
                    param.DPFInletTempRaw = p.getRawValue();
                    param.DPFInletTempHex = p.getHexRawValue();
                    break;
                case "EngMfldPress_kPa":
                    param.EngineIntakeManifoldPressure = p.getActualValues();
                    param.EngineIntakeManifoldPressureRaw = p.getRawValue();
                    param.EngineIntakeManifoldPressureHex = p.getHexRawValue();
                    break;
                case "DPF_PressDiff_kPa":
                    param.DPFPressureDifferential = p.getActualValues();
                    param.DPFPressureDifferentialRaw = p.getRawValue();
                    param.DPFPressureDifferentialHex = p.getHexRawValue();
                    break;
                case "EngCrnkCaseP_kPa":
                    param.EngineCrankcasePressure = p.getActualValues();
                    param.EngineCrankcasePressureRaw = p.getRawValue();
                    param.EngineCrankcasePressureHex = p.getHexRawValue();
                    break;
                case "TrboSpd_rpm":
                    param.EngineTurbochargerSpeed = p.getActualValues();
                    param.EngineTurbochargerSpeedRaw = p.getRawValue();
                    param.EngineTurbochargerSpeedHex = p.getHexRawValue();
                    break;
                case "EngFuelTemp_C":
                    param.FuelTemp = p.getActualValues();
                    param.FuelTempRaw = p.getRawValue();
                    param.FuelTempHex = p.getHexRawValue();
                    break;
                case "SCR_InNOx_ppm":
                    param.SCRInletNox = String.valueOf(p.getActualValues());
                    param.SCRInletNoxRaw = p.getRawValue();
                    param.SCRInletNoxHex = p.getHexRawValue();
                    break;
                case "SCR_OutNOx_ppm":
                    param.SCROutletNox = String.valueOf(p.getActualValues());
                    param.SCROutletNoxRaw = p.getRawValue();
                    param.SCROutletNoxHex = p.getHexRawValue();
                    break;
                case "TotPassRegen_cnt":
                    param.TotalNoOfPassiveRegenerations = p.getActualValues();
                    param.TotalNoOfPassiveRegenerationsRaw = p.getRawValue();
                    param.TotalNoOfPassiveRegenerationsHex = p.getHexRawValue();
                    break;
                case "DPF_Ash_%":
                    param.DPFAshLoad = String.valueOf(p.getActualValues());
                    param.DPFAshLoadRaw = p.getRawValue();
                    param.DPFAshLoadHex = p.getHexRawValue();
                    break;
                case "TotActRegens_cnt":
                    param.TotalNoOfActiveRegenerations = p.getActualValues();
                    param.TotalNoOfActiveRegenerationsRaw = p.getRawValue();
                    param.TotalNoOfActiveRegenerationsHex = p.getHexRawValue();
                    break;
                case "DPF_Soot_%":
                    param.DPFSootLoad = String.valueOf(p.getActualValues());
                    param.DPFSootLoadRaw = p.getRawValue();
                    param.DPFSootLoadHex = p.getHexRawValue();
                    break;
                case "Barometric_Pressure":
                    param.BarometricPressure = p.getActualValues();
                    param.BarometricPressureRaw = p.getRawValue();
                    param.BarometricPressureHex = p.getHexRawValue();
                    break;
                case "Fan_Drive_State":
                    param.FanState = String.valueOf(p.getActualValues());
                    param.FanStateRaw = p.getRawValue();
                    param.FanStateHex = p.getHexRawValue();
                    break;
            }

        }
        return param;
    }

    private String convertLowerCase(String value) {
        return value.trim().toLowerCase();
    }

    byte[] integerToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);
        return result;
    }

}
