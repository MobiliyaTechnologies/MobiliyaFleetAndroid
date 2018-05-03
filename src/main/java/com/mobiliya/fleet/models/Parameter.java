package com.mobiliya.fleet.models;

import com.mobiliya.fleet.db.tables.DB_BASIC;

import static com.mobiliya.fleet.utils.DateUtils.getLocalTimeString;


/**
 * Created by prashant on 02/20/2018.
 */

@SuppressWarnings({"ALL", "unused"})
public class Parameter extends DB_BASIC {
    public String TripId = "NA";
    public String TenantId = "NA";
    public String UserId = "";
    public String VehicleId = "";
    public String VehicleRegNumber = "NA";

    public String VIN = "NA";
    public int RPM = 0;
    public float Speed = 0;
    public int MaxSpeed = 0;
    public float HiResMaxSpeed = 0;
    public float Distance = 0;
    public float HiResDistance = 0;
    public float LoResDistance = 0;
    public float Odometer = 0;
    public float HiResOdometer = 0;
    public float LoResOdometer = 0;
    public float TotalHours = 0;
    public float IdleHours = 0;
    public int PctLoad = 0;
    public int PctTorque = 0;
    public int DrvPctTorque = 0;
    public String TorqueMode = "NA";
    public float FuelUsed = 0;
    public float HiResFuelUsed = 0;
    public float IdleFuelUsed = 0;
    public float FuelRate = 0;
    public float AvgFuelEcon = 0;
    public float InstFuelEcon = 0;
    public float PrimaryFuelLevel = 0;
    public float SecondaryFuelLevel = 0;
    public float OilTemp = 0;
    public float OilPressure = 0;
    public float TransTemp = 0;
    public float IntakeTemp = 0;
    public float IntakePressure = 0;
    public float CoolantTemp = 0;
    public float CoolantLevel = 0;
    public float CoolantPressure = 0;
    public float BrakeAppPressure = 0;
    public float Brake1AirPressure = 0;
    public float Brake2AirPressure = 0;
    public float AccelPedal = 0;
    public float ThrottlePos = 0;
    public float BatteryPotential = 0;
    public int SelectedGear = 0;
    public int CurrentGear = 0;
    public String Make = "NA";
    public String Model = "NA";
    public String SerialNo = "NA";
    public String UnitNo = "NA";
    public String EngineVIN = "NA";
    public String EngineMake = "NA";
    public String EngineModel = "NA";
    public String EngineSerialNo = "NA";
    public String EngineUnitNo = "NA";
    public String ClutchSwitch = "NA";
    public String BrakeSwitch = "NA";
    public String ParkBrakeSwitch = "NA";
    public float CruiseSetSpeed = -1;
    public String CruiseOnOff = "NA";
    public String CruiseSet = "NA";
    public String CruiseCoast = "NA";
    public String CruiseResume = "NA";
    public String CruiseAccel = "NA";
    public String CruiseActive = "NA";
    public String CruiseState = "NA";
    public String FaultSource = "0";
    public String FaultSPN = "0";
    public String FaultDescription = "0";
    public String FaultFMI = "0";
    public String FaultOccurrence = "0";
    public String FaultConversion = "false";
    public String Latitude;
    public String Longitude;
    public String ParameterDateTime = getLocalTimeString();
    public String AdapterId;
    public String FirmwareVersion;
    public String HardwareVersion;
    public String AdapterSerialNo;
    public String HardwareType;
    public boolean IsKeyOn;
    public String SleepMode;
    public int LedBrightness = 0;
    public String Message;
    public String Status;


    public int PGN = 0;
    public String PGNActualValue = "NA";
    public String PGNRawValue = "NA";
    public String PGNParameterName = "NA";
    public String PGNHexValue = "NA";
//public ArrayList<PGN> PGNParamList=new ArrayList<PGN>();

    public double AmbientTemp = 0;
    public double AmbientTempRaw = 0;
    public String AmbientTempHex = "NA";

    public double DPFOutletTemp = 0;
    public double DPFOutletTempRaw = 0;
    public String DPFOutletTempHex = "NA";

    public double EngineIntakeManifoldTemp = 0;
    public double EngineIntakeManifoldTempRaw = 0;
    public String EngineIntakeManifoldTempHex = "NA";

    public double DPFInletTemp = 0;
    public double DPFInletTempRaw = 0;
    public String DPFInletTempHex = "NA";

    public double EngineIntakeManifoldPressure = 0;
    public double EngineIntakeManifoldPressureRaw = 0;
    public String EngineIntakeManifoldPressureHex = "NA";

    public double DPFPressureDifferential = 0;
    public double DPFPressureDifferentialRaw = 0;
    public String DPFPressureDifferentialHex = "NA";

    public double EngineCrankcasePressure = -1;
    public double EngineCrankcasePressureRaw = -1;
    public String EngineCrankcasePressureHex = "NA";

    public double EngineTurbochargerSpeed = -1;
    public double EngineTurbochargerSpeedRaw = -1;
    public String EngineTurbochargerSpeedHex = "NA";

    public double FuelTemp = 0;
    public double FuelTempRaw = 0;
    public String FuelTempHex = "NA";

    public String SCRInletNox = "NA";
    public double SCRInletNoxRaw = 0;
    public String SCRInletNoxHex = "NA";

    public String SCROutletNox = "NA";
    public double SCROutletNoxRaw = -1;
    public String SCROutletNoxHex = "NA";

    public double TotalNoOfPassiveRegenerations = -1;
    public double TotalNoOfPassiveRegenerationsRaw = -1;
    public String TotalNoOfPassiveRegenerationsHex = "NA";

    public String DPFAshLoad = "NA";
    public double DPFAshLoadRaw = -1;
    public String DPFAshLoadHex = "NA";

    public double TotalNoOfActiveRegenerations = -1;
    public double TotalNoOfActiveRegenerationsRaw = -1;
    public String TotalNoOfActiveRegenerationsHex = "NA";

    public String DPFSootLoad = "NA";
    public double DPFSootLoadRaw = -1;
    public String DPFSootLoadHex = "NA";

    public double BarometricPressure = -1;
    public double BarometricPressureRaw = -1;
    public String BarometricPressureHex = "NA";

    public String FanState = "NA";
    public double FanStateRaw = -1;
    public String FanStateHex = "NA";

    public String TempPGNData = "";
    public String rawPGNData = "";

    public boolean isConnected = false;

    // added variables related to ODB2

    public String ResetOBD = "NA";
    public String EchoOff = "NA";
    public String LineFeedOff = "NA";
    public String CommandEquivalanceRatio = "NA";
    public String DistanceTraveledWithMILOn = "NA";
    public String DiagonosticTroubleCodes = "NA";
    public String TimingAdvance = "NA";
    public String ThrottlePosition = "NA";
    public String VehicleIdentificationNumber = "NA";
    public String EngineLoad = "NA";
    public String FuelType = "NA";
    public String FuelConsumptionRate = "NA";
    public String FuelLevel = "NA";
    public String LongTermFuelTrimBank1 = "NA";
    public String LongTermFuelTrimBank2 = "NA";
    public String ShortTermFuelTrimBank1 = "NA";
    public String ShortTermFuelTrimBank2 = "NA";
    public String FuelRation = "NA";
    public String IntakeMainfoldPressure = "NA";
    public String AirIntakeTemperature = "NA";
    public String EngineCoolantTemperature = "NA";
    public float VehicleSpeed = 0;
    public String MassAirFlow = "NA";
    public String AirFuelRatio = "NA";
    public String AmbientAirTempreture = "NA";
    public String ControlModulePowerSupply = "NA";
    public String DistanceSincecodescleared = "NA";
    public String EngineOilTempreture = "NA";
    public String EngineRPM = "NA";
    public String EngineRuntime = "NA";
    public String FuelPressure = "NA";
    public String FuelRailPressure = "NA";
    public String SelectProtocolAUTO = "NA";
    public String Timeout = "NA";
    public String TroubleCodes = "NA";
    public String WidebandAirFuelRatio = "NA";

}
