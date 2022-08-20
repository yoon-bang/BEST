# Raspberry Pi Coding

## Python-based beacon scanner
it collects beacon's minor ID in 10 seconds, and send uncollected beacon's minor ID to server.
Looping the function above.

### Useage
```
sudo python3 scanner.py
```

## Python-based temperature and humidity sensing program
Collects the temperature and humidity data, and sends to server.
if temperature and humidity is as close as fire situation, send HTTP Request to Server to notify fire.

### Useage 
```
python3 sensor.py
```

