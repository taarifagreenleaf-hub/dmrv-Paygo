"""
Generates test_data/dmrv_test_data.xlsx with four sheets:
  - Customers (20 rows)
  - Village_Chairmen (5 rows)
  - Locations (5 rows)
  - Devices (20 rows)

Run: python scripts/generate_test_data.py
"""

import os
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "test_data")
OUT_FILE = os.path.join(OUT_DIR, "dmrv_test_data.xlsx")

# ── Colour palette ──────────────────────────────────────────────────────────
GREEN_HEADER  = "1B5E20"   # dark green
BLUE_HEADER   = "0D47A1"   # dark blue
BROWN_HEADER  = "4E342E"   # dark brown
PURPLE_HEADER = "4A148C"   # dark purple
WHITE_TEXT    = "FFFFFF"
LIGHT_ROW_A   = "E8F5E9"
LIGHT_ROW_B   = "FFFFFF"

# ── Raw data ─────────────────────────────────────────────────────────────────

LOCATIONS = [
    {
        "location_id": "LOC-001",
        "name": "Karura Forest Reforestation",
        "description": "Urban forest restoration along Karura ridge, Nairobi.",
        "project_type": "reforestation",
        "country": "KE",
        "latitude": -1.2294,
        "longitude": 36.8267,
        "area_hectares": 250.0,
        "methodology": "VM0015 Methodology",
        "standard": "VCS",
        "village": "Karura Village",
        "county": "Nairobi County",
    },
    {
        "location_id": "LOC-002",
        "name": "Mida Creek Blue Carbon",
        "description": "Mangrove conservation and restoration in Mida Creek wetlands.",
        "project_type": "blue_carbon",
        "country": "KE",
        "latitude": -3.3667,
        "longitude": 40.1000,
        "area_hectares": 180.0,
        "methodology": "VM0033 Methodology",
        "standard": "VCS",
        "village": "Mida Village",
        "county": "Kilifi County",
    },
    {
        "location_id": "LOC-003",
        "name": "Lake Victoria Wetlands Carbon",
        "description": "Papyrus wetland protection reducing methane and storing carbon.",
        "project_type": "methane_capture",
        "country": "KE",
        "latitude": -0.1022,
        "longitude": 34.7617,
        "area_hectares": 320.0,
        "methodology": "AMS-III.U",
        "standard": "Gold Standard",
        "village": "Kajulu Village",
        "county": "Kisumu County",
    },
    {
        "location_id": "LOC-004",
        "name": "Bwindi Agroforestry Project",
        "description": "Smallholder agroforestry and soil carbon sequestration near Bwindi.",
        "project_type": "soil_carbon",
        "country": "UG",
        "latitude": -1.0333,
        "longitude": 29.6833,
        "area_hectares": 410.0,
        "methodology": "VM0042 Methodology",
        "standard": "VCS",
        "village": "Buhoma Village",
        "county": "Kanungu District",
    },
    {
        "location_id": "LOC-005",
        "name": "Kilimanjaro Cookstove Programme",
        "description": "Improved cookstove distribution reducing biomass consumption and emissions.",
        "project_type": "cookstoves",
        "country": "TZ",
        "latitude": -3.3667,
        "longitude": 36.6833,
        "area_hectares": 0.0,
        "methodology": "AMS-II.G",
        "standard": "Gold Standard",
        "village": "Moshi Village",
        "county": "Kilimanjaro Region",
    },
]

VILLAGE_CHAIRMEN = [
    {
        "chairman_id": "VC-001",
        "full_name": "Mwangi Kamau",
        "email": "mwangi.kamau@karura.ke",
        "password": "Chair@2024!",
        "phone_number": "+254701234501",
        "organization": "Karura Village Council",
        "location_id": "LOC-001",
        "location_name": "Karura Forest Reforestation",
        "village": "Karura Village",
        "national_id": "KE-NID-10234501",
        "role": "Village Chairman",
    },
    {
        "chairman_id": "VC-002",
        "full_name": "Fatuma Hassan",
        "email": "fatuma.hassan@mida.ke",
        "password": "Chair@2024!",
        "phone_number": "+254701234502",
        "organization": "Mida Village Council",
        "location_id": "LOC-002",
        "location_name": "Mida Creek Blue Carbon",
        "village": "Mida Village",
        "national_id": "KE-NID-10234502",
        "role": "Village Chairwoman",
    },
    {
        "chairman_id": "VC-003",
        "full_name": "Otieno Odhiambo",
        "email": "otieno.odhiambo@kajulu.ke",
        "password": "Chair@2024!",
        "phone_number": "+254701234503",
        "organization": "Kajulu Village Council",
        "location_id": "LOC-003",
        "location_name": "Lake Victoria Wetlands Carbon",
        "village": "Kajulu Village",
        "national_id": "KE-NID-10234503",
        "role": "Village Chairman",
    },
    {
        "chairman_id": "VC-004",
        "full_name": "Tumwine Bwambale",
        "email": "tumwine.bwambale@buhoma.ug",
        "password": "Chair@2024!",
        "phone_number": "+256701234504",
        "organization": "Buhoma Village Council",
        "location_id": "LOC-004",
        "location_name": "Bwindi Agroforestry Project",
        "village": "Buhoma Village",
        "national_id": "UG-NID-10234504",
        "role": "Village Chairman",
    },
    {
        "chairman_id": "VC-005",
        "full_name": "Josephat Minja",
        "email": "josephat.minja@moshi.tz",
        "password": "Chair@2024!",
        "phone_number": "+255701234505",
        "organization": "Moshi Village Council",
        "location_id": "LOC-005",
        "location_name": "Kilimanjaro Cookstove Programme",
        "village": "Moshi Village",
        "national_id": "TZ-NID-10234505",
        "role": "Village Chairman",
    },
]

CUSTOMERS = [
    # Nairobi / LOC-001
    {"customer_id": "CUST-001", "full_name": "Amina Wanjiru",      "email": "amina.wanjiru@greenearth.ke",    "password": "Test@1234!", "phone_number": "+254711000001", "organization": "GreenEarth Kenya",         "location_id": "LOC-001", "role": "Farmer"},
    {"customer_id": "CUST-002", "full_name": "Brian Otieno",        "email": "brian.otieno@farmlink.ke",       "password": "Test@1234!", "phone_number": "+254711000002", "organization": "FarmLink Ltd",             "location_id": "LOC-001", "role": "Field Agent"},
    {"customer_id": "CUST-003", "full_name": "Catherine Muthoni",   "email": "catherine.muthoni@ngo.ke",       "password": "Test@1234!", "phone_number": "+254711000003", "organization": "EcoAfrica NGO",            "location_id": "LOC-001", "role": "Project Developer"},
    {"customer_id": "CUST-004", "full_name": "David Njoroge",       "email": "david.njoroge@carbon.ke",        "password": "Test@1234!", "phone_number": "+254711000004", "organization": "Carbon Trust Kenya",       "location_id": "LOC-001", "role": "Verifier"},
    # Mombasa / LOC-002
    {"customer_id": "CUST-005", "full_name": "Esther Achieng",      "email": "esther.achieng@coastcarbon.ke",  "password": "Test@1234!", "phone_number": "+254711000005", "organization": "Coast Carbon Initiative",  "location_id": "LOC-002", "role": "Farmer"},
    {"customer_id": "CUST-006", "full_name": "Francis Barasa",      "email": "francis.barasa@marineeco.ke",    "password": "Test@1234!", "phone_number": "+254711000006", "organization": "Marine Ecosystems Ltd",    "location_id": "LOC-002", "role": "Field Agent"},
    {"customer_id": "CUST-007", "full_name": "Grace Chebet",        "email": "grace.chebet@blueocean.ke",      "password": "Test@1234!", "phone_number": "+254711000007", "organization": "Blue Ocean NGO",           "location_id": "LOC-002", "role": "Project Developer"},
    {"customer_id": "CUST-008", "full_name": "Hassan Mwenda",       "email": "hassan.mwenda@coastverify.ke",   "password": "Test@1234!", "phone_number": "+254711000008", "organization": "Coast Verify Co",          "location_id": "LOC-002", "role": "Verifier"},
    # Kisumu / LOC-003
    {"customer_id": "CUST-009", "full_name": "Irene Adhiambo",      "email": "irene.adhiambo@lakefarm.ke",     "password": "Test@1234!", "phone_number": "+254711000009", "organization": "Lake Farm Cooperative",    "location_id": "LOC-003", "role": "Farmer"},
    {"customer_id": "CUST-010", "full_name": "James Omondi",        "email": "james.omondi@victoriaeco.ke",    "password": "Test@1234!", "phone_number": "+254711000010", "organization": "Victoria Eco Trust",       "location_id": "LOC-003", "role": "Field Agent"},
    {"customer_id": "CUST-011", "full_name": "Kalinda Anyango",     "email": "kalinda.anyango@wetlands.ke",    "password": "Test@1234!", "phone_number": "+254711000011", "organization": "Wetlands Alliance",        "location_id": "LOC-003", "role": "Project Developer"},
    {"customer_id": "CUST-012", "full_name": "Lawrence Simiyu",     "email": "lawrence.simiyu@greenverify.ke", "password": "Test@1234!", "phone_number": "+254711000012", "organization": "GreenVerify Kenya",        "location_id": "LOC-003", "role": "Verifier"},
    # Bwindi / LOC-004
    {"customer_id": "CUST-013", "full_name": "Margaret Nakato",     "email": "margaret.nakato@soilfund.ug",    "password": "Test@1234!", "phone_number": "+256711000013", "organization": "Soil Carbon Fund UG",      "location_id": "LOC-004", "role": "Farmer"},
    {"customer_id": "CUST-014", "full_name": "Nicholas Ssebuyira",  "email": "nicholas.ssebuyira@agroug.ug",   "password": "Test@1234!", "phone_number": "+256711000014", "organization": "AgroUG Ltd",               "location_id": "LOC-004", "role": "Field Agent"},
    {"customer_id": "CUST-015", "full_name": "Olive Akello",        "email": "olive.akello@forestngo.ug",      "password": "Test@1234!", "phone_number": "+256711000015", "organization": "Forest NGO Uganda",        "location_id": "LOC-004", "role": "Project Developer"},
    {"customer_id": "CUST-016", "full_name": "Patrick Mugisha",     "email": "patrick.mugisha@carbonsouth.ug", "password": "Test@1234!", "phone_number": "+256711000016", "organization": "Carbon South Uganda",      "location_id": "LOC-004", "role": "Verifier"},
    # Kilimanjaro / LOC-005
    {"customer_id": "CUST-017", "full_name": "Rehema Masanja",      "email": "rehema.masanja@cookstove.tz",    "password": "Test@1234!", "phone_number": "+255711000017", "organization": "Cookstove Alliance TZ",    "location_id": "LOC-005", "role": "Farmer"},
    {"customer_id": "CUST-018", "full_name": "Samuel Kimaro",       "email": "samuel.kimaro@climateact.tz",    "password": "Test@1234!", "phone_number": "+255711000018", "organization": "ClimateAct Tanzania",      "location_id": "LOC-005", "role": "Field Agent"},
    {"customer_id": "CUST-019", "full_name": "Theresia Mollel",     "email": "theresia.mollel@kilienergy.tz",  "password": "Test@1234!", "phone_number": "+255711000019", "organization": "Kili Energy Foundation",   "location_id": "LOC-005", "role": "Project Developer"},
    {"customer_id": "CUST-020", "full_name": "Upendo Lyimo",        "email": "upendo.lyimo@tanzverify.tz",     "password": "Test@1234!", "phone_number": "+255711000020", "organization": "TanzVerify Ltd",           "location_id": "LOC-005", "role": "Verifier"},
]

DEVICES = [
    # Nairobi / LOC-001
    {"device_id": "DEV-001", "device_name": "KaruraSoilSensor-01",   "device_type": "soil_moisture",      "serial_number": "SM-KR-0001", "manufacturer": "AgriSense",    "model": "AS-SM200",  "firmware": "v2.1.3", "location_id": "LOC-001", "location_name": "Karura Forest Reforestation", "measurement_type": "soil_health",          "unit": "kg/m2",  "calibration_date": "2024-01-15", "status": "active"},
    {"device_id": "DEV-002", "device_name": "KaruraCO2Sensor-01",     "device_type": "co2_sensor",         "serial_number": "CO2-KR-0002","manufacturer": "SenseAir",     "model": "S8-LP",     "firmware": "v1.4.0", "location_id": "LOC-001", "location_name": "Karura Forest Reforestation", "measurement_type": "carbon_sequestration",  "unit": "tCO2e",  "calibration_date": "2024-01-20", "status": "active"},
    {"device_id": "DEV-003", "device_name": "KaruraWeather-01",       "device_type": "weather_station",    "serial_number": "WX-KR-0003", "manufacturer": "Davis",        "model": "Vantage Pro2","firmware": "v3.0.1","location_id": "LOC-001", "location_name": "Karura Forest Reforestation", "measurement_type": "custom",               "unit": "mm",     "calibration_date": "2024-02-01", "status": "active"},
    {"device_id": "DEV-004", "device_name": "KaruraBiomass-01",       "device_type": "biomass_sensor",     "serial_number": "BM-KR-0004", "manufacturer": "ForestSense",  "model": "FS-B100",   "firmware": "v1.0.5", "location_id": "LOC-001", "location_name": "Karura Forest Reforestation", "measurement_type": "carbon_sequestration",  "unit": "tCO2e",  "calibration_date": "2024-02-10", "status": "active"},
    # Mombasa / LOC-002
    {"device_id": "DEV-005", "device_name": "MidaWaterQuality-01",    "device_type": "water_quality",      "serial_number": "WQ-MC-0005", "manufacturer": "YSI",          "model": "Pro30",     "firmware": "v4.2.1", "location_id": "LOC-002", "location_name": "Mida Creek Blue Carbon",       "measurement_type": "water_quality",        "unit": "mg/L",   "calibration_date": "2024-01-10", "status": "active"},
    {"device_id": "DEV-006", "device_name": "MidaCO2Flux-01",         "device_type": "co2_flux_sensor",    "serial_number": "CF-MC-0006", "manufacturer": "LI-COR",       "model": "LI-8100A",  "firmware": "v6.0.0", "location_id": "LOC-002", "location_name": "Mida Creek Blue Carbon",       "measurement_type": "carbon_sequestration",  "unit": "tCO2e",  "calibration_date": "2024-01-25", "status": "active"},
    {"device_id": "DEV-007", "device_name": "MidaTidalGauge-01",      "device_type": "tidal_gauge",        "serial_number": "TG-MC-0007", "manufacturer": "OTT",          "model": "OTT PLS",   "firmware": "v2.3.0", "location_id": "LOC-002", "location_name": "Mida Creek Blue Carbon",       "measurement_type": "custom",               "unit": "m",      "calibration_date": "2024-03-01", "status": "active"},
    {"device_id": "DEV-008", "device_name": "MidaBiodiversity-01",    "device_type": "biodiversity_cam",   "serial_number": "BC-MC-0008", "manufacturer": "Bushnell",     "model": "Core S-4K", "firmware": "v1.1.0", "location_id": "LOC-002", "location_name": "Mida Creek Blue Carbon",       "measurement_type": "biodiversity",         "unit": "species","calibration_date": "2024-03-15", "status": "active"},
    # Kisumu / LOC-003
    {"device_id": "DEV-009", "device_name": "VictoriaMethane-01",     "device_type": "methane_sensor",     "serial_number": "CH4-LV-0009","manufacturer": "Aeroqual",     "model": "Series 500","firmware": "v3.1.2", "location_id": "LOC-003", "location_name": "Lake Victoria Wetlands Carbon","measurement_type": "emission_reduction",    "unit": "tCO2e",  "calibration_date": "2024-01-05", "status": "active"},
    {"device_id": "DEV-010", "device_name": "VictoriaSoilCarbon-01",  "device_type": "soil_carbon_probe",  "serial_number": "SC-LV-0010", "manufacturer": "METER Group",  "model": "TEROS 12",  "firmware": "v2.0.0", "location_id": "LOC-003", "location_name": "Lake Victoria Wetlands Carbon","measurement_type": "soil_health",          "unit": "g/kg",   "calibration_date": "2024-02-20", "status": "active"},
    {"device_id": "DEV-011", "device_name": "VictoriaWater-01",       "device_type": "water_quality",      "serial_number": "WQ-LV-0011", "manufacturer": "Hach",         "model": "HQ40d",     "firmware": "v5.0.1", "location_id": "LOC-003", "location_name": "Lake Victoria Wetlands Carbon","measurement_type": "water_quality",        "unit": "NTU",    "calibration_date": "2024-03-10", "status": "active"},
    {"device_id": "DEV-012", "device_name": "VictoriaDrone-01",       "device_type": "uav_sensor",         "serial_number": "UAV-LV-0012","manufacturer": "DJI",          "model": "Mavic 3E",  "firmware": "v07.01", "location_id": "LOC-003", "location_name": "Lake Victoria Wetlands Carbon","measurement_type": "biodiversity",         "unit": "ha",     "calibration_date": "2024-04-01", "status": "active"},
    # Bwindi / LOC-004
    {"device_id": "DEV-013", "device_name": "BwindiSoil-01",          "device_type": "soil_moisture",      "serial_number": "SM-BW-0013", "manufacturer": "METER Group",  "model": "5TM",       "firmware": "v1.2.1", "location_id": "LOC-004", "location_name": "Bwindi Agroforestry Project",  "measurement_type": "soil_health",          "unit": "kg/m2",  "calibration_date": "2024-01-18", "status": "active"},
    {"device_id": "DEV-014", "device_name": "BwindiCO2-01",           "device_type": "co2_sensor",         "serial_number": "CO2-BW-0014","manufacturer": "Vaisala",      "model": "GMP343",    "firmware": "v2.2.0", "location_id": "LOC-004", "location_name": "Bwindi Agroforestry Project",  "measurement_type": "carbon_sequestration",  "unit": "tCO2e",  "calibration_date": "2024-02-05", "status": "active"},
    {"device_id": "DEV-015", "device_name": "BwindiWeather-01",       "device_type": "weather_station",    "serial_number": "WX-BW-0015", "manufacturer": "Campbell Sci", "model": "CR300",     "firmware": "v4.0.0", "location_id": "LOC-004", "location_name": "Bwindi Agroforestry Project",  "measurement_type": "custom",               "unit": "°C",     "calibration_date": "2024-02-15", "status": "active"},
    {"device_id": "DEV-016", "device_name": "BwindiTreeHeight-01",    "device_type": "lidar_sensor",       "serial_number": "LD-BW-0016", "manufacturer": "Leica",        "model": "BLK360",    "firmware": "v1.0.2", "location_id": "LOC-004", "location_name": "Bwindi Agroforestry Project",  "measurement_type": "carbon_sequestration",  "unit": "tCO2e",  "calibration_date": "2024-03-05", "status": "active"},
    # Kilimanjaro / LOC-005
    {"device_id": "DEV-017", "device_name": "KiliStoveSensor-01",     "device_type": "combustion_sensor",  "serial_number": "CS-KL-0017", "manufacturer": "Aprovecho",    "model": "WBT-3.0",   "firmware": "v1.3.0", "location_id": "LOC-005", "location_name": "Kilimanjaro Cookstove Programme","measurement_type": "emission_reduction",   "unit": "tCO2e",  "calibration_date": "2024-01-22", "status": "active"},
    {"device_id": "DEV-018", "device_name": "KiliStoveSensor-02",     "device_type": "combustion_sensor",  "serial_number": "CS-KL-0018", "manufacturer": "Aprovecho",    "model": "WBT-3.0",   "firmware": "v1.3.0", "location_id": "LOC-005", "location_name": "Kilimanjaro Cookstove Programme","measurement_type": "emission_reduction",   "unit": "tCO2e",  "calibration_date": "2024-01-22", "status": "active"},
    {"device_id": "DEV-019", "device_name": "KiliAirQuality-01",      "device_type": "air_quality",        "serial_number": "AQ-KL-0019", "manufacturer": "Alphasense",   "model": "OPC-N3",    "firmware": "v2.0.0", "location_id": "LOC-005", "location_name": "Kilimanjaro Cookstove Programme","measurement_type": "emission_reduction",   "unit": "μg/m3",  "calibration_date": "2024-02-28", "status": "active"},
    {"device_id": "DEV-020", "device_name": "KiliGPSTracker-01",      "device_type": "gps_tracker",        "serial_number": "GPS-KL-0020","manufacturer": "Garmin",       "model": "inReach Mini","firmware": "v3.80", "location_id": "LOC-005", "location_name": "Kilimanjaro Cookstove Programme","measurement_type": "custom",               "unit": "coords", "calibration_date": "2024-03-20", "status": "active"},
]

# ── Styling helpers ──────────────────────────────────────────────────────────

def style_header_row(ws, row_num, hex_color, columns):
    fill = PatternFill("solid", fgColor=hex_color)
    font = Font(bold=True, color=WHITE_TEXT, size=11)
    align = Alignment(horizontal="center", vertical="center", wrap_text=True)
    thin = Side(style="thin", color="CCCCCC")
    border = Border(left=thin, right=thin, top=thin, bottom=thin)
    for col_idx in range(1, columns + 1):
        cell = ws.cell(row=row_num, column=col_idx)
        cell.fill = fill
        cell.font = font
        cell.alignment = align
        cell.border = border


def style_data_row(ws, row_num, columns, is_even):
    fill = PatternFill("solid", fgColor=LIGHT_ROW_A if is_even else LIGHT_ROW_B)
    align = Alignment(vertical="center", wrap_text=False)
    thin = Side(style="thin", color="DDDDDD")
    border = Border(left=thin, right=thin, top=thin, bottom=thin)
    for col_idx in range(1, columns + 1):
        cell = ws.cell(row=row_num, column=col_idx)
        cell.fill = fill
        cell.alignment = align
        cell.border = border


def freeze_and_fit(ws, columns):
    ws.freeze_panes = "A2"
    for col_idx in range(1, columns + 1):
        col_letter = get_column_letter(col_idx)
        ws.column_dimensions[col_letter].width = 22
    ws.row_dimensions[1].height = 30


def write_sheet(ws, headers, rows, hex_color):
    ws.append(headers)
    style_header_row(ws, 1, hex_color, len(headers))
    for i, row in enumerate(rows, start=2):
        ws.append(row)
        style_data_row(ws, i, len(headers), i % 2 == 0)
    freeze_and_fit(ws, len(headers))


# ── Build workbook ────────────────────────────────────────────────────────────

def build_workbook():
    wb = Workbook()
    wb.remove(wb.active)  # remove default sheet

    # ── Sheet 1: Locations ───────────────────────────────────────────────────
    ws_loc = wb.create_sheet("Locations")
    loc_headers = [
        "location_id", "name", "description", "project_type", "country",
        "latitude", "longitude", "area_hectares", "methodology", "standard",
        "village", "county",
    ]
    loc_rows = [[loc[h] for h in loc_headers] for loc in LOCATIONS]
    write_sheet(ws_loc, loc_headers, loc_rows, GREEN_HEADER)

    # ── Sheet 2: Village_Chairmen ────────────────────────────────────────────
    ws_vc = wb.create_sheet("Village_Chairmen")
    vc_headers = [
        "chairman_id", "full_name", "email", "password", "phone_number",
        "organization", "location_id", "location_name", "village",
        "national_id", "role",
    ]
    vc_rows = [[vc[h] for h in vc_headers] for vc in VILLAGE_CHAIRMEN]
    write_sheet(ws_vc, vc_headers, vc_rows, PURPLE_HEADER)

    # ── Sheet 3: Customers ───────────────────────────────────────────────────
    ws_cust = wb.create_sheet("Customers")
    cust_headers = [
        "customer_id", "full_name", "email", "password", "phone_number",
        "organization", "location_id", "role",
    ]
    cust_rows = [[c[h] for h in cust_headers] for c in CUSTOMERS]
    write_sheet(ws_cust, cust_headers, cust_rows, BLUE_HEADER)

    # ── Sheet 4: Devices ─────────────────────────────────────────────────────
    ws_dev = wb.create_sheet("Devices")
    dev_headers = [
        "device_id", "device_name", "device_type", "serial_number",
        "manufacturer", "model", "firmware", "location_id", "location_name",
        "measurement_type", "unit", "calibration_date", "status",
    ]
    dev_rows = [[d[h] for h in dev_headers] for d in DEVICES]
    write_sheet(ws_dev, dev_headers, dev_rows, BROWN_HEADER)

    os.makedirs(OUT_DIR, exist_ok=True)
    wb.save(OUT_FILE)
    print(f"Saved → {OUT_FILE}")
    print(f"  Locations      : {len(LOCATIONS)}")
    print(f"  Village Chairmen: {len(VILLAGE_CHAIRMEN)}")
    print(f"  Customers       : {len(CUSTOMERS)}")
    print(f"  Devices         : {len(DEVICES)}")


if __name__ == "__main__":
    build_workbook()
