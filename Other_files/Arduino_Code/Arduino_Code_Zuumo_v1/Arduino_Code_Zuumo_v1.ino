#include <stdio.h>
#include <ctype.h>
#include <Servo.h>

const int btLed = 4; // Pin to led
const int rxPin = 1; // Tx pin on BT unit
const int txPin = 0; // Rx pin on BT unit
const int servoPin = 7;

char command;
String string;
boolean party = false;
int turnOn = 0;
Servo myservo;
int pos = 0;

void setup() {
  Serial.begin(9600);   // Open serial communication to Serial Monitor
  pinMode(btLed, OUTPUT);
  myservo.attach(7);
  myservo.write(0);
}



void loop() {
  while (Serial.available()) {
    command = (char)Serial.read();
    if (command == ":") {
      break;
    } else {
      string += command;
    }
  delay(1); // DOES NOT WORK WITHOUT THIS..
  }
  Serial.print(" ");
  commandList(string);
  string = "";
}

void commandList(String string) {
  if (string == "LEDON") {
    //Serial.print("Hello");
    ledOn();
  }
  if (string == "LEDOFF") {
    ledOff();
  }
  if (string == "PARTY") {
    party = true;
  }
  if (string == "NOPARTY") {
    party = false;
  }
  if (string.substring(0, 2) == "AT") {
    //Serial.print(string);
  }
  if (string.substring(0, 6) == "SLIDER") {
    Serial.println(string.substring(6, string.length()));
    int pos2 = string.substring(6, string.length()).toInt();
    Serial.print(pos2);
    myservo.write(pos2);
    delay(2000); // might fix rx issues
  }
  houseParty(party); // Might be better to call all functions like this? (so ledOn/Off --> led(boolean b) )
  // So different tasks can run at the same time..
}

void ledOn() {
  digitalWrite(btLed, HIGH);
}

void ledOff() {
  digitalWrite(btLed, LOW);
}

void houseParty(boolean isParty) {
  if (isParty) {
    ledOn();
    delay(50);
    ledOff();
    delay(50);
  }
}

void servoTurn(int turnOn) {
  if (turnOn == 1) {
    for (pos = 0; pos <= 180; pos += 1) {
      myservo.write(pos);
      //delay(15);
    }
  }
  else if (turnOn == 2) {
    for (pos = 180; pos >= 0; pos -= 1) {
      myservo.write(pos);
      //delay(15);
    }
  }
  turnOn = 0;
}
