#include <stdio.h>
#include <ctype.h>
#include <Servo.h>

const int btLed = 4; // Pin to led
const int rxPin = 1; // Arduino transfer/bluetooth receive
const int txPin = 0; // Arduino receive/bluetooth transfer
const int servoPin = 7;

char command;
String string;
boolean party = false;
int turnOn = 0;
Servo myservo;
int pos = 0;

void setup() {
  Serial.begin(9600); //This is where you will transfer data. Open Serial Monitor to see data r/t.
  pinMode(btLed, OUTPUT);
  myservo.attach(7);
  myservo.write(0);

}

void loop() {
  if (Serial.available()) {
    string = "";
  }
  while (Serial.available()) {
    command = ((byte)Serial.read());
    if (command == ":") {
      break;
    } else {
      string += command;
    }
    delay(1); // DOES NOT WORK WITHOUT THIS..
  }
  if (string == "SERVOON") {
    turnOn = 1;
  }
  if (string == "SERVOOFF") {
    turnOn = 2;
  }
  if (turnOn == 1 || turnOn == 2) {
  if (turnOn == 1) {
      for (pos = 0; pos <= 45; pos += 1) {
        myservo.write(pos);
        delay(15);
      }
    }
    if (turnOn == 2) {
      for (pos = 45; pos >= 0; pos -= 1) {
        myservo.write(pos);
        delay(15);
      }
    }
  }
  turnOn = 0;
  commandList(string);
  string = "";
  Serial.print(" "); // DOES NOT WORK WITHOUT THIS..
}

void commandList(String string) {
  if (string == "LEDON") {
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
    Serial.print(string);
    //delay(2000); // might fix rx issues
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
