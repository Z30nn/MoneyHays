#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <ArduinoJson.h>
#include <ESP32Servo.h>

// Wi-Fi credentials
const char* ssid = "Hacker";
const char* password = "mikemike";
j
// Firebase credentials
#define API_KEY "AIzaSyCx1TrShYT1TU6el923icVlY4Kj_5wUdo8"
#define DATABASE_URL "https://coin-sorter-b3ffc-default-rtdb.asia-southeast1.firebasedatabase.app"

// Firebase objects
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

int potPin = 34;
Servo servo_26;

const int irPins[4] = {13, 12, 14, 27};
bool irStates[4] = {true, true, true, true};
int Peso1Count = 0, Peso5Count = 0, Peso10Count = 0, Peso20Count = 0, totalAmount = 0;
String activeUser = "";  // The username of the currently logged-in user

unsigned long lastUserCheck = 0;
const unsigned long userCheckInterval = 10000; // 10 seconds

void setup() {
  Serial.begin(9600);

  for (int i = 0; i < 4; i++) pinMode(irPins[i], INPUT);
  servo_26.attach(26);

  wifiSetup();
  firebaseSetup();
  checkActiveUserAndLoadCoins();
}

void loop() {
  // Check every 10 seconds for new active user
  if (millis() - lastUserCheck > userCheckInterval) {
    lastUserCheck = millis();
    checkActiveUserAndLoadCoins();
  }

  // Always sync the latest coin values in case of reset
  if (activeUser != "") {
    loadCoinData();
  }

  // Control servo with potentiometer
  int potValue = analogRead(potPin);
  int speedValue = map(potValue, 0, 4095, 180, 92); // ESP32 ADC 12-bit

  servo_26.write(speedValue);

  // Check IR sensors for coins
  for (int i = 0; i < 4; i++) {
    int currentState = digitalRead(irPins[i]);

    if (currentState == LOW && irStates[i] == true) {
      irStates[i] = false;

      if (activeUser == "") return;
      String basePath = "/users/" + activeUser + "/coinSorter/";

      switch (i) {
        case 0:
          Peso1Count++;
          Firebase.RTDB.setInt(&fbdo, basePath + "peso1", Peso1Count);
          Serial.println("Detected 1 Peso coin. Total 1P: " + String(Peso1Count));
          break;
        case 1:
          Peso5Count++;
          Firebase.RTDB.setInt(&fbdo, basePath + "peso5", Peso5Count);
          Serial.println("Detected 5 Peso coin. Total 5P: " + String(Peso5Count));
          break;
        case 2:
          Peso10Count++;
          Firebase.RTDB.setInt(&fbdo, basePath + "peso10", Peso10Count);
          Serial.println("Detected 10 Peso coin. Total 10P: " + String(Peso10Count));
          break;
        case 3:
          Peso20Count++;
          Firebase.RTDB.setInt(&fbdo, basePath + "peso20", Peso20Count);
          Serial.println("Detected 20 Peso coin. Total 20P: " + String(Peso20Count));
          break;
      }

      totalAmount = Peso1Count + (Peso5Count * 5) + (Peso10Count * 10) + (Peso20Count * 20);
      Firebase.RTDB.setInt(&fbdo, basePath + "total", totalAmount);
      Serial.println("Total amount: " + String(totalAmount) + " Pesos");
    }

    if (currentState == HIGH && irStates[i] == false) {
      irStates[i] = true;
    }
  }
}

void wifiSetup() {
  WiFi.begin(ssid, password);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(3000);
    Serial.print(".");
  }
  Serial.println("\nConnected!");
}

void firebaseSetup() {
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;

  if (Firebase.signUp(&config, &auth, "", "")) {
    Serial.println("Firebase anonymous sign-in successful");
  } else {
    Serial.printf("Firebase sign-up failed: %s\n", config.signer.signupError.message.c_str());
  }

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

void checkActiveUserAndLoadCoins() {
  if (Firebase.RTDB.getString(&fbdo, "/users/activeUser")) {
    String newActiveUser = fbdo.stringData();
    if (newActiveUser != "" && newActiveUser != activeUser) {
      activeUser = newActiveUser;
      Serial.println("Switched to active user: " + activeUser);
      loadCoinData();
    }
  } else {
    Serial.println("Failed to get active user: " + fbdo.errorReason());
  }
}

void loadCoinData() {
  if (activeUser == "") return;

  String basePath = "/users/" + activeUser + "/coinSorter/";

  if (Firebase.RTDB.getInt(&fbdo, basePath + "peso1")) Peso1Count = fbdo.intData();
  if (Firebase.RTDB.getInt(&fbdo, basePath + "peso5")) Peso5Count = fbdo.intData();
  if (Firebase.RTDB.getInt(&fbdo, basePath + "peso10")) Peso10Count = fbdo.intData();
  if (Firebase.RTDB.getInt(&fbdo, basePath + "peso20")) Peso20Count = fbdo.intData();

  totalAmount = Peso1Count + (Peso5Count * 5) + (Peso10Count * 10) + (Peso20Count * 20);

  Serial.println("Updated coin data: 1P=" + String(Peso1Count) + ", 5P=" + String(Peso5Count) +
                 ", 10P=" + String(Peso10Count) + ", 20P=" + String(Peso20Count) +
                 ", Total=" + String(totalAmount));
}