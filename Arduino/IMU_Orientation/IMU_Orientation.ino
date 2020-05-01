 // MPU-9250 Digital Motion Processing (DMP) Library
#include <SparkFunMPU9250-DMP.h>
// SD Library manages file and hardware control
#include <SD.h>
#include "config.h"

#define betaDef    0.038f    // 2 * proportional gain
#define sampleFreq  100.0f    // sample frequency in Hz

MPU9250_DMP imu; // Create an instance of the MPU9250_DMP class

/////////////////////
// SD Card Globals //
/////////////////////
bool sdCardPresent = false; // Keeps track of if SD card is plugged in
String logFileName; // Active logging file
String logFileBuffer; // Buffer for logged data. Max is set in config

float gyroBias[3] = {0.f, 0.f, 0.f};
float accelBias[3] = {0.f, 0.f, 0.f};
int i = 0;
int count, counter = 0;

volatile float beta = betaDef;                // 2 * proportional gain (Kp)
volatile float q0 = 1.0f, q1 = 0.0f, q2 = 0.0f, q3 = 0.0f;  // quaternion of sensor frame relative to auxiliary frame
float refquat[4] = {q0, q1, q2, q3};
float quat[4];
//float q_inv[4];
float quat_i[4];
float initial_q[4];
float final_q[4];
float rel_quat[4];
float qDot[4];
float euler[3];
const byte initial_orientation = '1';
const byte final_orientation = '2';

boolean initial = true;
boolean sessionActive = true;
boolean madgwick = true;
boolean printMadgwick = false;
boolean printEuler = false;
boolean calc_attitude = false;
boolean calibrate = false;
boolean printGyro = false;
boolean printAccel = false;

void setup() {

  //Initialize LED and interrupt input
  initHardware();
  //calibrate
  //calibrateMPU6500();

   // Initialize the MPU-9250. Should return true on success:
  if ( !initIMU() ) 
  {
    LOG_PORT.println("Error connecting to MPU-9250");
    while (1) ; // Loop forever if we fail to connect
    // LED will remain off in this state.
  }

  // Check for the presence of an SD card, and initialize it:
  if ( initSD() )
  {
    sdCardPresent = true;
    // Get the next, available log file name
    logFileName = nextLogFile(); 
  }

  //LOG_PORT.println("In setup...");

  Serial1.begin(9600);

  delay(1);

}

void loop() {

  //return data if requested
  //madgwick quaternion return
 
  // If new input is available on serial port  Check for bluetooth messages
  if (Serial1.available()) {
    char c = Serial1.read();
    LOG_PORT.write(c);
    checkMessage(c);
  }

  if ( LOG_PORT.available() )
  {
    char c = LOG_PORT.read();
    //Serial1.print(c);
    parseSerialInput(c); // parse it
  }
  
  if(sessionActive) {
  
    // Then check IMU for new data, and log it
    if ( !imu.fifoAvailable() ) // If no new data is available
      return;                   // return to the top of the loop
  
    // Read from the digital motion processor's FIFO
    if ( imu.dmpUpdateFifo() != INV_SUCCESS )
      return; // If it fails return to top
  
    if ( imu.updateCompass() != INV_SUCCESS)
      return; //If compass read fails return to top

    if(calibrate) {
      calibrateMPU6500();
    }

    if (madgwick) {

      float gyro_x = (imu.calcGyro(imu.gx) - gyroBias[0]);
      float gyro_y = (imu.calcGyro(imu.gy) - gyroBias[1]); 
      float gyro_z = (imu.calcGyro(imu.gz) - gyroBias[2]); 
      float accel_x = imu.calcAccel(imu.ax) - accelBias[0];
      float accel_y = imu.calcAccel(imu.ay) - accelBias[1];
      float accel_z = imu.calcAccel(imu.az) - accelBias[2];
      float mag_x = imu.calcMag(imu.mx) + 40.f;// + 60.f;
      float mag_y = imu.calcMag(imu.my) - 40.f;// - 64.f;
      float mag_z = imu.calcMag(imu.mz);// - 10.f;

      float ting = PI/180.f;
  
      MadgwickAHRSupdate(gyro_x*ting, gyro_y*ting, gyro_z*ting,
                      accel_x, accel_y, accel_z,
                    mag_y, mag_x, -(mag_z));
      //quat2euler(refquat, true);
  
      if (printMadgwick) {
        
        LOG_PORT.println(String(quat[0]) + ", " + String(quat[1]) + ", "
            + String(quat[2]) + ", " + String(quat[3]));
        

        if (counter >= 10) {
          String quatString = "!-" + String(q0) + "," + String(q1)
              + "," + String(q2) + "," + String(q2) + "-!";
          Serial1.print(quatString);
          counter = 0;
        } else {
          counter += 1;
        }
            
      }

      if (printEuler) {
        LOG_PORT.println(String(euler[0]) + ", " + String(euler[1]) + ", " + String(euler[2]));
      }
  
      if (printGyro) {
        LOG_PORT.println(String(gyro_x) + ", " + String(gyro_y) + ", "
                    + String(gyro_z));
      }

      if (printAccel) {
        LOG_PORT.println(String(accel_x) + ", " + String(accel_y) + ", " + String(accel_z));
      }
  
      if (calc_attitude) {
        getAttitude();
      }

      //Serial1.print("1:" + q0 + "2:" + q1 "3:" + q2 "4:" + q3);
    }
      
  }
  
}

void initHardware(void)
{
  // Set up LED pin (active-high, default to off)
  pinMode(HW_LED_PIN, OUTPUT);
  digitalWrite(HW_LED_PIN, LOW);

  // Set up MPU-9250 interrupt input (active-low)
  pinMode(MPU9250_INT_PIN, INPUT_PULLUP);

  // Set up serial log port
  LOG_PORT.begin(9600);
  while(!LOG_PORT);
}

bool initSD(void)
{
  // SD.begin should return true if a valid SD card is present
  if ( !SD.begin(SD_CHIP_SELECT_PIN) )
  {
    return false;
  }

  return true;
}

bool initIMU(void)
{
  // imu.begin() should return 0 on success. Will initialize
  // I2C bus, and reset MPU-9250 to defaults.
  if (imu.begin() != INV_SUCCESS)
    return false;

  // Set up MPU-9250 interrupt:
  imu.enableInterrupt(); // Enable interrupt output
  imu.setIntLevel(1);    // Set interrupt to active-low
  imu.setIntLatched(1);  // Latch interrupt output

  // Configure sensors:
  // Set gyro full-scale range: options are 250, 500, 1000, or 2000:
  imu.setGyroFSR(1000);
  // Set accel full-scale range: options are 2, 4, 8, or 16 g 
  imu.setAccelFSR(4);
  // Set gyro/accel LPF: options are 5, 10, 20, 42, 98, 188 Hz
  imu.setLPF(20); 
  // Set gyro/accel sample rate: must be between 4-1000Hz
  // (note: this value will be overridden by the DMP sample rate)
  imu.setSampleRate(1000); 
  // Set compass sample rate: between 4-100Hz
  imu.setCompassSampleRate(100); 

  // Configure digital motion processor. Use the FIFO to get
  // data from the DMP.
  unsigned short dmpFeatureMask = 0;
  
  // Add raw gyro readings to the DMP
  //dmpFeatureMask |= DMP_FEATURE_SEND_RAW_GYRO;
  //dmpFeatureMask |= DMP_FEATURE_GYRO_CAL;
  dmpFeatureMask |= DMP_FEATURE_SEND_RAW_GYRO;
  
  // Add raw accel readings to the DMP
  dmpFeatureMask |= DMP_FEATURE_SEND_RAW_ACCEL;

  // Initialize the DMP, and set the FIFO's update rate:
  imu.dmpBegin(dmpFeatureMask, 100);

  return true; // Return success
}

void checkMessage(char in) {

  //LOG_PORT.println("HERE");

  switch(in) {
//    case '1':
//      sessionActive = true;
//      LOG_PORT.println("Session active");
//      break;
//      
//    case '2':
//      sessionActive = false;
//      LOG_PORT.println("should stop now");
//      //calibrateMPU6500();
//      break;
      
//    case '3':
//      sessionActive = true;
//      LOG_PORT.println("End function");
//      break;

//    case '1':
//      initial = true;
//      calc_attitude = true;
//      break;
//
//    case '2':
//      initial = false;
//      quatCpy(initial_q, quat_i);
//      calc_attitude = true;
//      //relativeQuaternion(initial_q, final_q);
//      break;
//
    case '3':
      madgwick = false;
      calibrate = true;
      break;

    case '4':
      printMadgwick = true;
      break;
      
    default:
      break;
  }
}

void calibrateMPU6500(void) {

  float imu_ax, imu_ay, imu_az = 0.0;
  float imu_gx, imu_gy, imu_gz = 0.0;

  imu_ax = imu.calcAccel(imu.ax);
  imu_ay = imu.calcAccel(imu.ay);
  imu_az = imu.calcAccel(imu.az);
  imu_gx = imu.calcGyro(imu.gx);
  imu_gy = imu.calcGyro(imu.gy);
  imu_gz = imu.calcGyro(imu.gz);
  
  if (count == 0) {
    accelBias[0], accelBias[1], accelBias[2], gyroBias[0], gyroBias[1], gyroBias[2] = 0.f;
  }

  accelBias[0] += imu_ax;
  accelBias[1] += imu_ay;
  if (imu_az > 0) {
    accelBias[2] += imu_az - 1.f;
  } else { accelBias[2] += imu_az + 1.f; }
  gyroBias[0] += imu_gx;
  gyroBias[1] += imu_gy;
  gyroBias[2] += imu_gz;

  count += 1;
  
  if (count == 300) {

    accelBias[0] /= 300.f;
    accelBias[1] /= 300.f;
    accelBias[2] /= 300.f;
    gyroBias[0] /= 300.f;
    gyroBias[1] /= 300.f;
    gyroBias[2] /= 300.f;
    count = 0;
    if ( LOG_PORT.available() ) {
      LOG_PORT.println(String(accelBias[0]) + ", " + String(accelBias[1]) + ", " + String(accelBias[2]) + ", " 
                  + String(gyroBias[0]) + ", " + String(gyroBias[1]) + ", " + String(gyroBias[2]) + ", ");
      }
    calibrate = false;
    madgwick = true;
    delay(100);
    
  }
}

void quat2euler(float quat[4], bool degs) {

  float q0 = quat[0];
  float q1 = quat[1];
  float q2 = quat[2];
  float q3 = quat[3];
  
  float sin_phi = 2.0f*(q0*q1 + q2*q3);
  float cos_phi = q0*q0 - q1*q1 - q2*q2 + q3*q3;//1.0f - 2.0f*(q1*q1 + q2*q2);
  float phi = atan2(sin_phi, cos_phi);

  float theta = asin(2.0f*(q0*q2 - q3*q1));

  float sin_psi = 2.0f*(q0*q3 + q1*q2);
  float cos_psi = q0*q0 - q1*q1 - q2*q2 - q3*q3;//1.0f - 2.0f*(q2*q2 + q3*q3);
  float psi = atan2(sin_psi, cos_psi);

  if (degs) {
    phi *= (180.0 / PI);
    theta *= (180.0 / PI);
    psi *= (180.0 / PI);
    if (phi < 0) phi = abs(phi);
    if (theta < 0) theta = abs(theta);
    if (psi < 0) psi = abs(psi); 
  }

  euler[0] = phi;   //roll
  euler[1] = theta; //pitch
  euler[2] = psi;   //yaw
  
  String Roll = String(phi);
  String Pitch = String(theta);
  String Yaw = String(psi);
  char charBuf[30];
  String btMessage = "R:" + Roll + " P:" + Pitch + " Y:" + Yaw;
  btMessage.toCharArray(charBuf, 30);
  String output = "Euler values: Pitch: " + String(theta) + " - Roll: " 
              + String(phi) + " - Yaw: " + String(psi);
  LOG_PORT.println(output);
  
  Serial1.write(charBuf);
}

void relativeQuaternion(float initial_quat[4], float final_quat[4]) {
  //get inverse of final quaternion
  float inv_quat[4];
  inverse_quat(inv_quat, final_quat);

  //get dot product of inverse of final orientation quaternion and initial orientation quaternion
  qCrossProduct(rel_quat, inv_quat, initial_quat);

  LOG_PORT.println("Relative quaternion is: " + String(rel_quat[0]) + ", "
                + String(rel_quat[1]) + ", " + String(rel_quat[2]) + ", " + String(rel_quat[3])); 

  quat2euler(rel_quat, true);
  
}

void inverse_quat(float (&q_inv)[4], float quatern[4]) {
  q_inv[0] = quatern[0];
  q_inv[1] = -quatern[1];
  q_inv[2] = -quatern[2];
  q_inv[3] = -quatern[3];
  
}

void qCrossProduct(float (&qDot)[4], float q1[4], float q2[4]) {
  qDot[0] = q1[0]*q2[0] - q1[1]*q2[1] - q1[2]*q2[2] - q1[3]*q2[3];
  qDot[1] = q1[0]*q2[1] + q1[1]*q2[0] + q1[2]*q2[3] - q1[3]*q2[2];
  qDot[2] = q1[0]*q2[2] - q1[1]*q2[3] + q1[2]*q2[0] - q1[3]*q2[1];
  qDot[3] = q1[0]*q2[3] + q1[1]*q2[2] - q1[2]*q2[1] - q1[3]*q2[0];
}

void getAttitude() {

  if (count == 0) {
    quat_i[0] = 0.f;
    quat_i[1] = 0.f;
    quat_i[2] = 0.f;
    quat_i[3] = 0.f;  
  }
  
  quat_i[0] += quat[0];
  quat_i[1] += quat[1];
  quat_i[2] += quat[2];
  quat_i[3] += quat[3];

  count += 1;

  if (count == 300) {
    quat_i[0] /= 300.f;
    quat_i[1] /= 300.f;
    quat_i[2] /= 300.f;
    quat_i[3] /= 300.f;
    count = 0;
    calc_attitude = false;

    LOG_PORT.println("Attitude calculated as: " + String(quat[0]) + ", "
                + String(quat[1]) + ", " + String(quat[2]) + ", " + String(quat[3]));

    if(!initial) {
      quatCpy(final_q, quat_i);
      relativeQuaternion(initial_q, final_q);
    }
  }
}
  
void parseSerialInput(char c) {
  
  switch(c) {

    case 'o':
      initial = true;
      calc_attitude = true;
      break;

    case 'f':
      initial = false;
      quatCpy(initial_q, quat_i);
      calc_attitude = true;
      break;

    case 'r':
      quatCpy(final_q, quat_i);
      relativeQuaternion(initial_q, final_q);
      //quat2euler(rel_quat, true);
      break;

    case 'm':
      printMadgwick = !printMadgwick;
      break;

    case 'c':
      madgwick = false;
      calibrate = true;
      break;

    case 'g':
      printGyro =!printGyro;
      break;

    case 'a':
      printAccel = !printAccel;
      break;

    case 'e':
      printEuler = !printEuler;
      break;

    case 'q':
      {
      //LOG_PORT.println(q0);
      String quatString = String(q0) + "," + String(q1) + "," + String(q2) + "," + String(q2);
      Serial1.print(quatString); //Serial1.print(q1+ ",");
//      Serial1.print(q2+ ","); Serial1.print(q3);
      }
      break;
      
    default:
      break;
  }
}

void quatCpy(float q1[4], float q2[4]) {
  q1[0] = q2[0];
  q1[1] = q2[1];
  q1[2] = q2[2];
  q1[3] = q2[3];
}

void MadgwickAHRSupdate(float Gx, float Gy, float Gz, float Ax, float Ay, float Az, float Mx, float My, float Mz) {
  float recipNorm;
  float s0, s1, s2, s3;
  float qDot1, qDot2, qDot3, qDot4;
  float hx, hy;
  float _2q0mx, _2q0my, _2q0mz, _2q1mx, _2bx, _2bz, _4bx, _4bz, _2q0, _2q1, _2q2, _2q3, _2q0q2, _2q2q3, q0q0, q0q1, q0q2, q0q3, q1q1, q1q2, q1q3, q2q2, q2q3, q3q3;

  // Use IMU algorithm if magnetometer measurement invalid (avoids NaN in magnetometer normalisation)
  if((Mx == 0.0f) && (My == 0.0f) && (Mz == 0.0f)) {
    MadgwickAHRSupdateIMU(Gx, Gy, Gz, Ax, Ay, Az);
    return;
  }

  // Rate of change of quaternion from gyroscope
  qDot1 = 0.5f * (-q1 * Gx - q2 * Gy - q3 * Gz);
  qDot2 = 0.5f * (q0 * Gx + q2 * Gz - q3 * Gy);
  qDot3 = 0.5f * (q0 * Gy - q1 * Gz + q3 * Gx);
  qDot4 = 0.5f * (q0 * Gz + q1 * Gy - q2 * Gx);

  // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
  if(!((Ax == 0.0f) && (Ay == 0.0f) && (Az == 0.0f))) {

    // Normalise accelerometer measurement
    recipNorm = invSqrt(Ax * Ax + Ay * Ay + Az * Az);
    Ax *= recipNorm;
    Ay *= recipNorm;
    Az *= recipNorm;   

    // Normalise magnetometer measurement
    recipNorm = invSqrt(Mx * Mx + My * My + Mz * Mz);
    Mx *= recipNorm;
    My *= recipNorm;
    Mz *= recipNorm;

    // Auxiliary variables to avoid repeated arithmetic
    _2q0mx = 2.0f * q0 * Mx;
    _2q0my = 2.0f * q0 * My;
    _2q0mz = 2.0f * q0 * Mz;
    _2q1mx = 2.0f * q1 * Mx;
    _2q0 = 2.0f * q0;
    _2q1 = 2.0f * q1;
    _2q2 = 2.0f * q2;
    _2q3 = 2.0f * q3;
    _2q0q2 = 2.0f * q0 * q2;
    _2q2q3 = 2.0f * q2 * q3;
    q0q0 = q0 * q0;
    q0q1 = q0 * q1;
    q0q2 = q0 * q2;
    q0q3 = q0 * q3;
    q1q1 = q1 * q1;
    q1q2 = q1 * q2;
    q1q3 = q1 * q3;
    q2q2 = q2 * q2;
    q2q3 = q2 * q3;
    q3q3 = q3 * q3;

    // Reference direction of Earth's magnetic field
    hx = Mx * q0q0 - _2q0my * q3 + _2q0mz * q2 + Mx * q1q1 + _2q1 * My * q2 + _2q1 * Mz * q3 - Mx * q2q2 - Mx * q3q3;
    hy = _2q0mx * q3 + My * q0q0 - _2q0mz * q1 + _2q1mx * q2 - My * q1q1 + My * q2q2 + _2q2 * Mz * q3 - My * q3q3;
    _2bx = sqrt(hx * hx + hy * hy);
    _2bz = -_2q0mx * q2 + _2q0my * q1 + Mz * q0q0 + _2q1mx * q3 - Mz * q1q1 + _2q2 * My * q3 - Mz * q2q2 + Mz * q3q3;
    _4bx = 2.0f * _2bx;
    _4bz = 2.0f * _2bz;

    // Gradient decent algorithm corrective step
    s0 = -_2q2 * (2.0f * q1q3 - _2q0q2 - Ax) + _2q1 * (2.0f * q0q1 + _2q2q3 - Ay) - _2bz * q2 * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - Mx) + (-_2bx * q3 + _2bz * q1) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - My) + _2bx * q2 * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - Mz);
    s1 = _2q3 * (2.0f * q1q3 - _2q0q2 - Ax) + _2q0 * (2.0f * q0q1 + _2q2q3 - Ay) - 4.0f * q1 * (1 - 2.0f * q1q1 - 2.0f * q2q2 - Az) + _2bz * q3 * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - Mx) + (_2bx * q2 + _2bz * q0) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - My) + (_2bx * q3 - _4bz * q1) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - Mz);
    s2 = -_2q0 * (2.0f * q1q3 - _2q0q2 - Ax) + _2q3 * (2.0f * q0q1 + _2q2q3 - Ay) - 4.0f * q2 * (1 - 2.0f * q1q1 - 2.0f * q2q2 - Az) + (-_4bx * q2 - _2bz * q0) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - Mx) + (_2bx * q1 + _2bz * q3) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - My) + (_2bx * q0 - _4bz * q2) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - Mz);
    s3 = _2q1 * (2.0f * q1q3 - _2q0q2 - Ax) + _2q2 * (2.0f * q0q1 + _2q2q3 - Ay) + (-_4bx * q3 + _2bz * q1) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - Mx) + (-_2bx * q0 + _2bz * q2) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - My) + _2bx * q1 * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - Mz);
    recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
    s0 *= recipNorm;
    s1 *= recipNorm;
    s2 *= recipNorm;
    s3 *= recipNorm;

    // Apply feedback step
    qDot1 -= beta * s0;
    qDot2 -= beta * s1;
    qDot3 -= beta * s2;
    qDot4 -= beta * s3;
  }

  // Integrate rate of change of quaternion to yield quaternion
  q0 += qDot1 * (1.0f / sampleFreq);
  q1 += qDot2 * (1.0f / sampleFreq);
  q2 += qDot3 * (1.0f / sampleFreq);
  q3 += qDot4 * (1.0f / sampleFreq);

  // Normalise quaternion
  recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
  q0 *= recipNorm;
  q1 *= recipNorm;
  q2 *= recipNorm;
  q3 *= recipNorm;

  quat[0] = q0;
  quat[1] = q1;
  quat[2] = q2;
  quat[3] = q3;
}

void MadgwickAHRSupdateIMU(float Gx, float Gy, float Gz, float Ax, float Ay, float Az) {
  float recipNorm;
  float s0, s1, s2, s3;
  float qDot1, qDot2, qDot3, qDot4;
  float _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2 ,_8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

  // Rate of change of quaternion from gyroscope
  qDot1 = 0.5f * (-q1 * Gx - q2 * Gy - q3 * Gz);
  qDot2 = 0.5f * (q0 * Gx + q2 * Gz - q3 * Gy);
  qDot3 = 0.5f * (q0 * Gy - q1 * Gz + q3 * Gx);
  qDot4 = 0.5f * (q0 * Gz + q1 * Gy - q2 * Gx);

  // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
  if(!((Ax == 0.0f) && (Ay == 0.0f) && (Az == 0.0f))) {

    // Normalise accelerometer measurement
    recipNorm = invSqrt(Ax * Ax + Ay * Ay + Az * Az);
    Ax *= recipNorm;
    Ay *= recipNorm;
    Az *= recipNorm;   

    // Auxiliary variables to avoid repeated arithmetic
    _2q0 = 2.0f * q0;
    _2q1 = 2.0f * q1;
    _2q2 = 2.0f * q2;
    _2q3 = 2.0f * q3;
    _4q0 = 4.0f * q0;
    _4q1 = 4.0f * q1;
    _4q2 = 4.0f * q2;
    _8q1 = 8.0f * q1;
    _8q2 = 8.0f * q2;
    q0q0 = q0 * q0;
    q1q1 = q1 * q1;
    q2q2 = q2 * q2;
    q3q3 = q3 * q3;

    // Gradient decent algorithm corrective step
    s0 = _4q0 * q2q2 + _2q2 * Ax + _4q0 * q1q1 - _2q1 * Ay;
    s1 = _4q1 * q3q3 - _2q3 * Ax + 4.0f * q0q0 * q1 - _2q0 * Ay - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * Az;
    s2 = 4.0f * q0q0 * q2 + _2q0 * Ax + _4q2 * q3q3 - _2q3 * Ay - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * Az;
    s3 = 4.0f * q1q1 * q3 - _2q1 * Ax + 4.0f * q2q2 * q3 - _2q2 * Ay;
    recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
    s0 *= recipNorm;
    s1 *= recipNorm;
    s2 *= recipNorm;
    s3 *= recipNorm;

    // Apply feedback step
    qDot1 -= beta * s0;
    qDot2 -= beta * s1;
    qDot3 -= beta * s2;
    qDot4 -= beta * s3;
  }

  // Integrate rate of change of quaternion to yield quaternion
  q0 += qDot1 * (1.0f / sampleFreq);
  q1 += qDot2 * (1.0f / sampleFreq);
  q2 += qDot3 * (1.0f / sampleFreq);
  q3 += qDot4 * (1.0f / sampleFreq);

  // Normalise quaternion
  recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
  q0 *= recipNorm;
  q1 *= recipNorm;
  q2 *= recipNorm;
  q3 *= recipNorm;

  quat[0] = q0;
  quat[1] = q1;
  quat[2] = q2;
  quat[3] = q3;
}

float invSqrt(float x) {
  float halfx = 0.5f * x;
  float y = x;
  long i = *(long*)&y;
  i = 0x5f3759df - (i>>1);
  y = *(float*)&i;
  y = y * (1.5f - (halfx * y * y));
  return y;
}

// Log a string to the SD card
bool sdLogString(String toLog)
{
  // Open the current file name:
  File logFile = SD.open(logFileName, FILE_WRITE);
  
  // If the file will get too big with this new string, create
  // a new one, and open it.
  if (logFile.size() > (SD_MAX_FILE_SIZE - toLog.length()))
  {
    logFileName = nextLogFile();
    logFile = SD.open(logFileName, FILE_WRITE);
  }

  // If the log file opened properly, add the string to it.
  if (logFile)
  {
    logFile.print(toLog);
    logFile.close();

    return true; // Return success
  }

  return false; // Return fail
}

// Find the next available log file. Or return a null string
// if we've reached the maximum file limit.
String nextLogFile(void)
{
  String filename;
  int logIndex = 0;

  for (int i = 0; i < LOG_FILE_INDEX_MAX; i++)
  {
    // Construct a file with PREFIX[Index].SUFFIX
    filename = String(LOG_FILE_PREFIX);
    filename += String(logIndex);
    filename += ".";
    filename += String(LOG_FILE_SUFFIX);
    // If the file name doesn't exist, return it
    if (!SD.exists(filename))
    {
      return filename;
    }
    // Otherwise increment the index, and try again
    logIndex++;
  }

  return "";
}
