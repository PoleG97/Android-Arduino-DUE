#define HC06 Serial3
String phrase;

void setup()
{
  delay(1000);
  Serial.begin(9600);
  HC06.begin(9600);
  
  Serial.write("\nIniciamos Test\n");
}

void loop()
{
  while(HC06.available())
  {
    char data = HC06.read();
    phrase = String(phrase + data);
    
    Serial.write(data);
    //Serial.println(phrase);
  }
}
