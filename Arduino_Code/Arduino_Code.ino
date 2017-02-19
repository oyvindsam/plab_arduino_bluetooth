#include <stdio.h>
#include <ctype.h>

const int btLed = 4;
const int rxPin = 1;
const int txPin = 0;

char command;
String string;
int teller = 0;

void setup() {
  Serial.begin(9600);
  pinMode(btLed, OUTPUT);

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
    delay(1);
  }
  if (string == "LEDON") {
    LEDOn();
  }
  if (string == "LEDOFF") {
    LEDOff();
  }
  if (string.substring(0, 2) == "AT") {
    Serial.print(string);
    delay(2000); // might fix rx issues
  } else {
    Serial.print(" ");
  }
}

void LEDOn() {
  digitalWrite(btLed, HIGH);
}

void LEDOff() {
  digitalWrite(btLed, LOW);
  delay(300);
}

