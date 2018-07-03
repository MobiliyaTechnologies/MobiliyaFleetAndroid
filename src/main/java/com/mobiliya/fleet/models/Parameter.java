package com.mobiliya.fleet.models;

import com.mobiliya.fleet.db.tables.DB_BASIC;

import static com.mobiliya.fleet.utils.DateUtils.getLocalTimeString;


public class Parameter extends DB_BASIC {
    public String TripId = null;
    public String TenantId = null;
    public String UserId = null;
    public String VehicleId = null;
    public String VehicleRegNumber = null;

    public String VIN = null;
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
    public String TorqueMode = null;
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
    public String Make = null;
    public String Model = null;
    public String SerialNo = null;
    public String UnitNo = null;
    public String EngineVIN = null;
    public String EngineMake = null;
    public String EngineModel = null;
    public String EngineSerialNo = null;
    public String EngineUnitNo = null;
    public String ClutchSwitch = null;
    public String BrakeSwitch = null;
    public String ParkBrakeSwitch = null;
    public float CruiseSetSpeed = -1;
    public String CruiseOnOff = null;
    public String CruiseSet = null;
    public String CruiseCoast = null;
    public String CruiseResume = null;
    public String CruiseAccel = null;
    public String CruiseActive = null;
    public String CruiseState = null;
    public String FaultSource = "0";
    public String FaultSPN = "0";
    public String FaultDescription = "0";
    public String FaultFMI = "0";
    public String FaultOccurrence = "0";
    public String FaultConversion = "false";
    public String Latitude=null;
    public String Longitude=null;
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
    public String PGNActualValue = null;
    public String PGNRawValue = null;
    public String PGNParameterName = null;
    public String PGNHexValue = null;
//public ArrayList<PGN> PGNParamList=new ArrayList<PGN>();

    public double AmbientTemp = 0;
    public double AmbientTempRaw = 0;
    public String AmbientTempHex = null;

    public double DPFOutletTemp = 0;
    public double DPFOutletTempRaw = 0;
    public String DPFOutletTempHex = null;

    public double EngineIntakeManifoldTemp = 0;
    public double EngineIntakeManifoldTempRaw = 0;
    public String EngineIntakeManifoldTempHex = null;

    public double DPFInletTemp = 0;
    public double DPFInletTempRaw = 0;
    public String DPFInletTempHex = null;

    public double EngineIntakeManifoldPressure = 0;
    public double EngineIntakeManifoldPressureRaw = 0;
    public String EngineIntakeManifoldPressureHex = null;

    public double DPFPressureDifferential = 0;
    public double DPFPressureDifferentialRaw = 0;
    public String DPFPressureDifferentialHex = null;

    public double EngineCrankcasePressure = -1;
    public double EngineCrankcasePressureRaw = -1;
    public String EngineCrankcasePressureHex = null;

    public double EngineTurbochargerSpeed = -1;
    public double EngineTurbochargerSpeedRaw = -1;
    public String EngineTurbochargerSpeedHex = null;

    public double FuelTemp = 0;
    public double FuelTempRaw = 0;
    public String FuelTempHex = null;

    public String SCRInletNox = null;
    public double SCRInletNoxRaw = 0;
    public String SCRInletNoxHex = null;

    public String SCROutletNox = null;
    public double SCROutletNoxRaw = -1;
    public String SCROutletNoxHex = null;

    public double TotalNoOfPassiveRegenerations = -1;
    public double TotalNoOfPassiveRegenerationsRaw = -1;
    public String TotalNoOfPassiveRegenerationsHex = null;

    public String DPFAshLoad = null;
    public double DPFAshLoadRaw = -1;
    public String DPFAshLoadHex = null;

    public double TotalNoOfActiveRegenerations = -1;
    public double TotalNoOfActiveRegenerationsRaw = -1;
    public String TotalNoOfActiveRegenerationsHex = null;

    public String DPFSootLoad = null;
    public double DPFSootLoadRaw = -1;
    public String DPFSootLoadHex = null;

    public double BarometricPressure = -1;
    public double BarometricPressureRaw = -1;
    public String BarometricPressureHex = null;

    public String FanState = null;
    public double FanStateRaw = -1;
    public String FanStateHex = null;

    public String TempPGNData = null;
    public String rawPGNData = null;

    public boolean isConnected = false;

    // added variables related to ODB2

    public String ResetOBD = null;
    public String EchoOff = null;
    public String LineFeedOff = null;
    public String CommandEquivalanceRatio = null;
    public String DistanceTraveledWithMILOn = null;
    public String DiagonosticTroubleCodes = null;
    public String TimingAdvance = null;
    public String ThrottlePosition = null;
    public String VehicleIdentificationNumber = null;
    public String EngineLoad = null;
    public String FuelType = null;
    public String FuelConsumptionRate = null;
    public String FuelLevel = null;
    public String LongTermFuelTrimBank1 = null;
    public String LongTermFuelTrimBank2 = null;
    public String ShortTermFuelTrimBank1 = null;
    public String ShortTermFuelTrimBank2 = null;
    public String FuelRation = null;
    public String IntakeMainfoldPressure = null;
    public String AirIntakeTemperature = null;
    public String EngineCoolantTemperature = null;
    public float VehicleSpeed = 0;
    public String MassAirFlow = null;
    public String AirFuelRatio = null;
    public String AmbientAirTempreture = null;
    public String ControlModulePowerSupply = null;
    public String DistanceSincecodescleared = null;
    public String EngineOilTempreture = null;
    public String EngineRPM = null;
    public String EngineRuntime = null;
    public String FuelPressure = null;
    public String FuelRailPressure = null;
    public String SelectProtocolAUTO = null;
    public String Timeout = null;
    public String TroubleCodes = null;
    public String WidebandAirFuelRatio = null;
}
