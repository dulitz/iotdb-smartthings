/**
 *  Homerun
 *
 *  Copyright 2017 Daniel Dulitz
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Original Author: David Janes
 *  IOTDB.org
 *  2014-02-01
 *
 *  modified by Daniel Dulitz, 2017
 *
 *  Allow control of your SmartThings via an API; 
 *  Allow monitoring of your SmartThings.
 *
 *  Follow us on Twitter:
 *  - @iotdb
 *  - @dpjanes
 */
 
definition(
    name: "Homerun",
    namespace: "dulitz",
    author: "David Janes, modified by Daniel Dulitz",
    description: "Exposes endpoints to allow my programmatic controller access to SmartThings data.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true) {
		appSetting "uri"
		appSetting "user"
		appSetting "password"
    }
    

/* --- setup section --- */
/*
 *  The user 
 *  Make sure that if you change anything related to this in the code
 *  that you update the preferences in your installed apps.
 *
 *  Note that there's a SmartThings magic that's _required_ here,
 *  in that you cannot access a device unless you have it listed
 *  in the preferences. Access to those devices is given through
 *  the name used here (i.e. d_*)
 */
preferences {
    section("Allow homerun to control and access these things:") {
        input "d_switch", "capability.switch", title: "Switch", required: false, multiple: true
        input "d_motion", "capability.motionSensor", title: "Motion", required: false, multiple: true
        input "d_temperature", "capability.temperatureMeasurement", title: "Temperature", required: false, multiple: true
        input "d_contact", "capability.contactSensor", title: "Contact", required: false, multiple: true
        input "d_acceleration", "capability.accelerationSensor", title: "Acceleration", required: false, multiple: true
        input "d_presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
        input "d_battery", "capability.battery", title: "Battery", required: false, multiple: true
        input "d_threeAxis", "capability.threeAxis", title: "3 Axis", required: false, multiple: true
        input "d_humidity", "capability.relativeHumidityMeasurement", title: "Hygrometer", required: false, multiple: true
        input "d_water", "capability.waterSensor", title: "Water Sensor", required:false, multiple: true
		input "d_smoke", "capability.smokeDetector", title: "Smoke Detector", required:false, multiple: true
		input "d_carbonMonoxide", "capability.carbonMonoxideDetector", title: "Carbon Monoxide Detector", required:false, multiple: true
		input "d_power", "capability.powerMeter", title: "Power Meter", required:false, multiple: true
		input "d_outlet", "capability.outlet", title: "Outlet", required:false, multiple: true
        input "d_lqi", "capability.signalStrength", title: "Signal Strength", required:false, multiple: true
    }
    section("Report events at this location to the following HTTPS URI:") {
    	input "d_event_callback_uri", "text", title: "HTTPS URI", required:true, multiple:false
    }
}

/*
input "d_alarm", "capability.alarm", title: "alarm", multiple: true
input "d_configuration", "capability.configuration", title: "configuration", multiple: true
input "d_illuminanceMeasurement", "capability.illuminanceMeasurement", title: "illuminanceMeasurement", multiple: true
input "d_polling", "capability.polling", title: "polling", multiple: true
input "d_thermostatCoolingSetpoint", "capability.thermostatCoolingSetpoint", title: "thermostatCoolingSetpoint", multiple: true
input "d_thermostatFanMode", "capability.thermostatFanMode", title: "thermostatFanMode", multiple: true
input "d_thermostatHeatingSetpoint", "capability.thermostatHeatingSetpoint", title: "thermostatHeatingSetpoint", multiple: true
input "d_thermostatMode", "capability.thermostatMode", title: "thermostatMode", multiple: true
input "d_thermostatSetpoint", "capability.thermostatSetpoint", title: "thermostatSetpoint", multiple: true
input "d_threeAxisMeasurement", "capability.threeAxisMeasurement", title: "threeAxisMeasurement", multiple: true
lqi: 100 %
acceleration: inactive
threeAxis: -38,55,1021
battery: 88 %
temperature: 65 F
*/

/*
 *  The API
 */
mappings {
    path("/:type") {
        action: [
            GET: "_api_list"
        ]
    }
    path("/:type/:id") {
        action: [
            GET: "_api_get",
            PUT: "_api_put"
        ]
    }
}

/*
 *  This function is called once when the app is installed
 */

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

/*
 *  This function is called every time the user changes
 *  their preferences
 */
def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	_event_subscribe()
}

/* --- event section --- */

/*
 *  What events are we interested in. This needs
 *  to be in its own function because both
 *  updated() and installed() are interested in it.
 *
 * see http://docs.smartthings.com/en/latest/capabilities-reference.html#capabilities-taxonomy
 */
def _event_subscribe() {
    subscribe(d_switch, "switch", "_on_event")
    subscribe(d_motion, "motion", "_on_event")
    subscribe(d_temperature, "temperature", "_on_event")
    subscribe(d_humidity, "humidity", "_on_event")
    subscribe(d_lqi, "lqi", "_on_event")
    subscribe(d_contact, "contact", "_on_event")
    subscribe(d_acceleration, "acceleration", "_on_event")
    subscribe(d_presence, "presence", "_on_event")
    subscribe(d_battery, "battery", "_on_event")
    subscribe(d_threeAxis, "threeAxis", "_on_event")
    subscribe(d_outlet, "outlet", "_on_event")
    subscribe(d_water, "water", "_on_event")
    subscribe(d_smoke, "smoke", "_on_event")
    subscribe(d_carbonMonoxide, "carbonMonoxide", "_on_event")
    subscribe(d_power, "powerMeter", "_on_event")
}

/*
 *  This function is called whenever something changes.
 */
def _on_event(evt) {
//    log.debug "_on_event XXX event.id=${evt?.id} event.deviceId=${evt?.deviceId} event.isStateChange=${evt?.isStateChange} event.name=${evt?.name}"
    
    def dt = _device_and_type_for_event(evt)
    if (!dt) {
        log.debug "_on_event deviceId=${evt.deviceId} not found?"
        return;
    }
    
    def jd = _device_to_json(dt.device, dt.type)
    log.debug "_on_event deviceId=${jd}"

    _send_homerun(dt.device, dt.type, jd)

}

/* --- API section --- */
def _api_list() {
    _devices_for_type(params.type).collect {
        _device_to_json(it, params.type)
    }
}

def _api_put() {
    def devices = _devices_for_type(params.type)
    def device = devices.find { it.id == params.id }
    if (!device) {
        httpError(404, "Device not found")
    } else {
        _device_command(device, params.type, request.JSON)
    }
}

def _api_get() {
    def devices = _devices_for_type(params.type)
    def device = devices.find { it.id == params.id }
    if (!device) {
        httpError(404, "Device not found")
    } else {
        _device_to_json(device, params.type)
    }
}

void _api_update() {
    _do_update(_devices_for_type(params.type), params.type)
}

/*
 *  I don't know what this does but it seems to be needed
 */
def deviceHandler(evt) {
}

/* --- communication section --- */


/*
 *  Send information to my personal bridge
 */
def _send_homerun(device, device_type, deviced) {
    log.debug "_send_homerun called";

    def now = Calendar.instance
    def date = now.time
    def millis = date.time
    def sequence = millis
    def isodatetime = deviced?.value?.timestamp
    
/*	
    def sha256Hash = { text ->  
	    java.security.MessageDigest.getInstance("SHA-256")
	    .digest(text.getBytes("UTF-8")).encodeBase64().toString()  
    }
    def digest = "${appSettings.user}/${appSettings.password}/${isodatetime}/${sequence}".toString()
    def hash = sha256Hash(digest)
 */
 	def hash = "".toString()
    def topic = "st/${device_type}/${deviced.id}".toString()
    
    def uri = d_event_callback_uri
    if (!uri?.startsWith("https://")) uri = appSettings.uri
    def headers = [:]
    def body = [
        "topic": topic,
        "payload": deviced?.value,
        "timestamp": isodatetime,
        "sequence": sequence,
        "signed": hash,
        "username": appSettings.user,
        "password": appSettings.password  /* this is not in the clear as we're using TLS */
    ]

    def params = [
        uri: uri,
        headers: headers,
        body: body
    ]

    log.debug "_send_homerun: params=${params}"
    httpPutJson(params) { log.debug "_send_homerun: response=${response}" }
}



/* --- internals --- */
/*
 *  Devices and Types Dictionary
 */
def _dtd()
{
    [ 
        switch: d_switch, 
        motion: d_motion, 
        temperature: d_temperature, 
        contact: d_contact,
        acceleration: d_acceleration,
        presence: d_presence,
        battery: d_battery,
        threeAxis: d_threeAxis,
        humidity: d_humidity,
		outlet: d_outlet,
		water: d_water,
		smoke: d_smoke,
		carbonMonoxide: d_carbonMonoxide,
		powerMeter: d_power,
        lqi: d_lqi,

    ]
}

def _devices_for_type(type) 
{
    _dtd()[type]
}

def _device_and_type_for_event(evt)
{
    def dtd = _dtd()
    
    for (dt in _dtd()) {
        if (dt.key != evt.name) {
        	continue
        }
        
        def devices = dt.value
        for (device in devices) {
            if (device.id == evt.deviceId) {
                return [ device: device, type: dt.key ]
            }
        }
    }
}

/**
 *  Do a device command
 */
private _device_command(device, type, jsond) {
    if (!device) {
        return;
    }
    if (!jsond) {
        return;
    }
    
    if (type == "switch" || type == "outlet") {
        def n = jsond['switch']
        if (n == -1) {
            def o = device.currentState('switch')?.value
            n = ( o != 'on' )
        }
        if (n) {
            device.on()
        } else {
            device.off()
        }
    } else {
        log.debug "_device_command: device type=${type} doesn't take commands"
    }
}

/*
 *  Convert a single device into a JSONable object
 */
private _device_to_json(device, type) {
    if (!device) {
        return;
    }

    def vd = [:]
    def jd = [id: device.id, label: device.label, type: type, value: vd, hub: device.hub.name];
    
    if (type == "switch" || type == "outlet") {
        def s = device.currentState('switch')
        vd['timestamp'] = s?.isoDate
        vd['switch'] = s?.value == "on"
    } else if (type == "motion") {
        def s = device.currentState('motion')
        vd['timestamp'] = s?.isoDate
        vd['motion'] = s?.value == "active"
    } else if (type == "temperature") {
        def s = device.currentState('temperature')
        vd['timestamp'] = s?.isoDate
        vd['temperature'] = s?.value.toFloat()
    } else if (type == "contact") {
        def s = device.currentState('contact')
        vd['timestamp'] = s?.isoDate
        vd['contact'] = s?.value == "closed"
    } else if (type == "acceleration") {
        def s = device.currentState('acceleration')
        vd['timestamp'] = s?.isoDate
        vd['acceleration'] = s?.value == "active"
    } else if (type == "presence") {
        def s = device.currentState('presence')
        vd['timestamp'] = s?.isoDate
        vd['presence'] = s?.value == "present"
    } else if (type == "battery") {
        def s = device.currentState('battery')
        vd['timestamp'] = s?.isoDate
        vd['battery'] = s?.value.toFloat() / 100.0
     } else if (type == "humidity") {
        def s = device.currentState('humidity')
        vd['timestamp'] = s?.isoDate
        vd['humidity'] = s?.value.toFloat() / 100.0
    } else if (type == "threeAxis") {
        def s = device.currentState('threeAxis')
        vd['timestamp'] = s?.isoDate
        vd['x'] = s?.xyzValue?.x
        vd['y'] = s?.xyzValue?.y
        vd['z'] = s?.xyzValue?.z
    } else if (type == "power") {
		def s = device.currentState('power')
		vd['timestamp'] = s?.isoDate
		vd['power'] = s?.value.toFloat()
    } else if (type == "energy") {
		def s = device.currentState('energy')
		vd['timestamp'] = s?.isoDate
		vd['energy'] = s?.value.toFloat()
    } else if (type == "smoke") {
		def s = device.currentState('smoke')
		vd['timestamp'] = s?.isoDate
		vd['smoke'] = s?.smoke?.value == 'detected'
		vd['carbonMonoxide'] = s?.carbonMonoxide?.value == 'detected'
		vd['smoke_tested'] = s?.smoke?.value == 'tested'
		vd['carbonMonoxide_tested'] = s?.carbonMonoxide?.value == 'tested'
    } else if (type == "water") {
		def s = device.currentState('water')
		vd['timestamp'] = s?.isoDate
		vd['water'] = s?.value == 'wet'
    }

    return jd
}
