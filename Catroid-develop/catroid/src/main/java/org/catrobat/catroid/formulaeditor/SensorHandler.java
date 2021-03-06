/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2021 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.formulaeditor;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.bluetooth.base.BluetoothDevice;
import org.catrobat.catroid.bluetooth.base.BluetoothDeviceService;
import org.catrobat.catroid.camera.FaceTextPoseDetector;
import org.catrobat.catroid.cast.CastManager;
import org.catrobat.catroid.common.CatroidService;
import org.catrobat.catroid.common.ServiceProvider;
import org.catrobat.catroid.devices.arduino.phiro.Phiro;
import org.catrobat.catroid.devices.mindstorms.ev3.LegoEV3;
import org.catrobat.catroid.devices.mindstorms.nxt.LegoNXT;
import org.catrobat.catroid.nfc.NfcHandler;
import org.catrobat.catroid.utils.TouchUtil;

import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.VisibleForTesting;

public final class SensorHandler implements SensorEventListener, SensorCustomEventListener, LocationListener,
		GpsStatus.Listener {
	private static final float RADIAN_TO_DEGREE_CONST = 180f / (float) Math.PI;
	private static final String TAG = SensorHandler.class.getSimpleName();
	private static SensorHandler instance = null;
	private static BluetoothDeviceService btService = ServiceProvider.getService(CatroidService.BLUETOOTH_DEVICE_SERVICE);
	private SensorManagerInterface sensorManager = null;
	private Sensor linearAccelerationSensor = null;
	private Sensor accelerometerSensor = null;
	private Sensor magneticFieldSensor = null;
	private Sensor rotationVectorSensor = null;
	private float[] rotationMatrix = new float[16];
	private float[] rotationVector = new float[3];
	private float[] accelerationXYZ = new float[3];
	private float signAccelerationZ = 0f;
	private float[] gravity = new float[] {0f, 0f, 0f};
	private boolean useLinearAccelerationFallback = false;
	private boolean useRotationVectorFallback = false;
	private float linearAccelerationX = 0f;
	private float linearAccelerationY = 0f;
	private float linearAccelerationZ = 0f;
	private float loudness = 0f;
	private float firstFaceDetected = 0f;
	private float firstFaceSize = 0f;
	private float firstFacePositionX = 0f;
	private float firstFacePositionY = 0f;
	private float secondFaceDetected = 0f;
	private float secondFaceSize = 0f;
	private float secondFacePositionX = 0f;
	private float secondFacePositionY = 0f;
	private float noseX = 0f;
	private float noseY = 0f;
	private float leftEyeInnerX = 0f;
	private float leftEyeInnerY = 0f;
	private float leftEyeCenterX = 0f;
	private float leftEyeCenterY = 0f;
	private float leftEyeOuterX = 0f;
	private float leftEyeOuterY = 0f;
	private float rightEyeInnerX = 0f;
	private float rightEyeInnerY = 0f;
	private float rightEyeCenterX = 0f;
	private float rightEyeCenterY = 0f;
	private float rightEyeOuterX = 0f;
	private float rightEyeOuterY = 0f;
	private float leftEarX = 0f;
	private float leftEarY = 0f;
	private float rightEarX = 0f;
	private float rightEarY = 0f;
	private float mouthLeftCornerX = 0f;
	private float mouthLeftCornerY = 0f;
	private float mouthRightCornerX = 0f;
	private float mouthRightCornerY = 0f;
	private float leftShoulderX = 0f;
	private float leftShoulderY = 0f;
	private float rightShoulderX = 0f;
	private float rightShoulderY = 0f;
	private float leftElbowX = 0f;
	private float leftElbowY = 0f;
	private float rightElbowX = 0f;
	private float rightElbowY = 0f;
	private float leftWristX = 0f;
	private float leftWristY = 0f;
	private float rightWristX = 0f;
	private float rightWristY = 0f;
	private float leftPinkyKnuckleX = 0f;
	private float leftPinkyKnuckleY = 0f;
	private float rightPinkyKnuckleX = 0f;
	private float rightPinkyKnuckleY = 0f;
	private float leftIndexKnuckleX = 0f;
	private float leftIndexKnuckleY = 0f;
	private float rightIndexKnuckleX = 0f;
	private float rightIndexKnuckleY = 0f;
	private float leftThumbKnuckleX = 0f;
	private float leftThumbKnuckleY = 0f;
	private float rightThumbKnuckleX = 0f;
	private float rightThumbKnuckleY = 0f;
	private float leftHipX = 0f;
	private float leftHipY = 0f;
	private float rightHipX = 0f;
	private float rightHipY = 0f;
	private float leftKneeX = 0f;
	private float leftKneeY = 0f;
	private float rightKneeX = 0f;
	private float rightKneeY = 0f;
	private float leftAnkleX = 0f;
	private float leftAnkleY = 0f;
	private float rightAnkleX = 0f;
	private float rightAnkleY = 0f;
	private float leftHeelX = 0f;
	private float leftHeelY = 0f;
	private float rightHeelX = 0f;
	private float rightHeelY = 0f;
	private float leftFootIndexX = 0f;
	private float leftFootIndexY = 0f;
	private float rightFootIndexX = 0f;
	private float rightFootIndexY = 0f;
	private float textBlocksNumber = 0f;
	private String textFromCamera = "0";

	private boolean compassAvailable = true;
	private boolean accelerationAvailable = true;
	private boolean inclinationAvailable = true;

	private LocationManager locationManager = null;
	private boolean isGpsConnected = false;
	private Location lastLocationGps;
	private long lastLocationGpsMillis;
	private double latitude = 0d;
	private double longitude = 0d;
	private float locationAccuracy = 0f;
	private double altitude = 0d;
	private static String userLocaleTag = Locale.getDefault().toLanguageTag();
	public static double timerReferenceValue = 0d;
	public static double timerPauseValue = 0d;

	private static String listeningLanguageSensor;

	private SensorLoudness sensorLoudness = null;

	public static void setUserLocaleTag(String userLocale) {
		userLocaleTag = userLocale;
	}

	private SensorHandler(Context context) {
		sensorManager = new SensorManager(
				(android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE));
		linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		if (linearAccelerationSensor == null) {
			useLinearAccelerationFallback = true;
		}

		rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		if (rotationVectorSensor == null) {
			useRotationVectorFallback = true;
		}

		if (useRotationVectorFallback) {
			accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			if (accelerometerSensor == null) {
				accelerationAvailable = false;
				inclinationAvailable = false;
			}
			if (magneticFieldSensor == null) {
				compassAvailable = false;
			}
		} else if (useLinearAccelerationFallback) {
			accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			if (accelerometerSensor == null) {
				accelerationAvailable = false;
			}
		}
	}

	public boolean compassAvailable() {
		return this.compassAvailable;
	}

	public void setLocationManager(LocationManager locationManager) {
		this.locationManager = locationManager;
	}

	public static boolean gpsAvailable() {
		return gpsSensorAvailable() | networkGpsAvailable();
	}

	private static boolean gpsSensorAvailable() {
		return instance.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	private static boolean networkGpsAvailable() {
		return instance.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	private static double startWeekWithMonday() {
		int weekdayOfAndroidCalendar = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		int convertedWeekday;

		if (weekdayOfAndroidCalendar == Calendar.SUNDAY) {
			convertedWeekday = Calendar.SATURDAY;
		} else {
			convertedWeekday = weekdayOfAndroidCalendar - 1;
		}

		return convertedWeekday;
	}

	public boolean accelerationAvailable() {
		return this.accelerationAvailable;
	}

	public boolean inclinationAvailable() {
		return this.inclinationAvailable;
	}

	public static SensorHandler getInstance(Context context) {
		if (instance == null) {
			instance = new SensorHandler(context);
		}
		return instance;
	}

	@VisibleForTesting
	public void setAccelerationUnavailable() {
		accelerationAvailable = false;
	}

	@SuppressWarnings({"MissingPermission"})
	public static void startSensorListener(Context context) {
		if (instance == null) {
			instance = new SensorHandler(context);
		}
		instance.sensorManager.unregisterListener((SensorEventListener) instance);

		SensorHandler.registerListener(instance);

		FaceTextPoseDetector.addListener(instance);

		if (instance.sensorLoudness != null) {
			instance.sensorLoudness.registerListener(instance);
		}

		if (instance.locationManager != null) {
			instance.locationManager.removeUpdates(instance);
			instance.locationManager.removeGpsStatusListener(instance);
			instance.locationManager.addGpsStatusListener(instance);
			if (gpsSensorAvailable()) {
				instance.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, instance);
			}
			if (networkGpsAvailable()) {
				instance.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, instance);
			}
		}
	}

	public void setSensorLoudness(SensorLoudness sensorLoudness) {
		this.sensorLoudness = sensorLoudness;
	}

	public static void registerListener(SensorEventListener listener) {
		if (instance == null) {
			return;
		}

		if (!instance.useLinearAccelerationFallback) {
			instance.sensorManager.registerListener(listener, instance.linearAccelerationSensor,
					android.hardware.SensorManager.SENSOR_DELAY_GAME);
		}

		if (!instance.useRotationVectorFallback) {
			instance.sensorManager.registerListener(listener, instance.rotationVectorSensor,
					android.hardware.SensorManager.SENSOR_DELAY_GAME);
		}

		if (instance.useLinearAccelerationFallback || instance.useRotationVectorFallback) {
			instance.sensorManager.registerListener(listener, instance.accelerometerSensor,
					android.hardware.SensorManager.SENSOR_DELAY_GAME);
		}

		if (instance.useRotationVectorFallback && instance.magneticFieldSensor != null) {
			instance.sensorManager.registerListener(listener, instance.magneticFieldSensor,
					android.hardware.SensorManager.SENSOR_DELAY_GAME);
		}
	}

	public static void unregisterListener(SensorEventListener listener) {
		if (instance == null) {
			return;
		}
		instance.sensorManager.unregisterListener(listener);
	}

	public static void stopSensorListeners() {
		if (instance == null) {
			return;
		}
		instance.sensorManager.unregisterListener((SensorEventListener) instance);

		if (instance.sensorLoudness != null) {
			instance.sensorLoudness.unregisterListener(instance);
		}

		if (instance.locationManager != null) {
			instance.locationManager.removeUpdates(instance);
			instance.locationManager.removeGpsStatusListener(instance);
		}

		FaceTextPoseDetector.removeListener(instance);
	}

	public static Object getSensorValue(Sensors sensor) {
		if (instance.sensorManager == null) {
			return 0d;
		}
		Double sensorValue;
		float[] rotationMatrixOut = new float[16];
		int rotate;

		switch (sensor) {
			case X_ACCELERATION:
				return getXAccordingToRotation(instance.linearAccelerationX,
						instance.linearAccelerationY);
			case Y_ACCELERATION:
				return getYAccordingToRotation(instance.linearAccelerationX,
						instance.linearAccelerationY);
			case Z_ACCELERATION:
				return (double) instance.linearAccelerationZ;

			case COMPASS_DIRECTION:
				float[] orientations = new float[3];
				if (!instance.useRotationVectorFallback) {
					android.hardware.SensorManager.getRotationMatrixFromVector(instance.rotationMatrix,
							instance.rotationVector);
				}
				if ((rotate = rotateOrientation()) == 1) {
					android.hardware.SensorManager.remapCoordinateSystem(instance.rotationMatrix, android.hardware.SensorManager
							.AXIS_Y, android.hardware.SensorManager.AXIS_MINUS_X, rotationMatrixOut);
					android.hardware.SensorManager.getOrientation(rotationMatrixOut, orientations);
				} else if (rotate == -1) {
					android.hardware.SensorManager.remapCoordinateSystem(instance.rotationMatrix, android.hardware.SensorManager
							.AXIS_MINUS_Y, android.hardware.SensorManager.AXIS_X, rotationMatrixOut);
					android.hardware.SensorManager.getOrientation(rotationMatrixOut, orientations);
				} else {
					android.hardware.SensorManager.getOrientation(instance.rotationMatrix, orientations);
				}
				sensorValue = (double) orientations[0];
				return sensorValue * RADIAN_TO_DEGREE_CONST * -1f;

			case LATITUDE:
				return instance.latitude;

			case LONGITUDE:
				return instance.longitude;

			case LOCATION_ACCURACY:
				return (double) instance.locationAccuracy;

			case ALTITUDE:
				return instance.altitude;

			case USER_LANGUAGE:
				return userLocaleTag;

			case X_INCLINATION:
				if (instance.useRotationVectorFallback) {
					float rawInclinationX;
					if ((rotate = rotateOrientation()) != 0) {
						rawInclinationX = RADIAN_TO_DEGREE_CONST * (float) (Math.acos(instance
								.accelerationXYZ[1] * rotate));
					} else {
						rawInclinationX = RADIAN_TO_DEGREE_CONST * (float) (Math.acos(instance.accelerationXYZ[0]));
					}
					float correctedInclinationX = 0;

					if (rawInclinationX >= 90 && rawInclinationX <= 180) {
						if (instance.signAccelerationZ > 0) {
							correctedInclinationX = -(rawInclinationX - 90);
						} else {
							correctedInclinationX = -(180 + (90 - rawInclinationX));
						}
					} else if (rawInclinationX >= 0 && rawInclinationX < 90) {
						if (instance.signAccelerationZ > 0) {
							correctedInclinationX = (90 - rawInclinationX);
						} else {
							correctedInclinationX = (90 + rawInclinationX);
						}
					}
					if (rotateOrientation() != 0) {
						correctedInclinationX = -correctedInclinationX;
					}
					return (double) correctedInclinationX;
				} else {
					orientations = new float[3];
					android.hardware.SensorManager.getRotationMatrixFromVector(instance.rotationMatrix,
							instance.rotationVector);
					if ((rotate = rotateOrientation()) == 1) {
						android.hardware.SensorManager.remapCoordinateSystem(instance.rotationMatrix, android.hardware.SensorManager
								.AXIS_Y, android.hardware.SensorManager.AXIS_MINUS_X, rotationMatrixOut);
						android.hardware.SensorManager.getOrientation(rotationMatrixOut, orientations);
					} else if (rotate == -1) {
						android.hardware.SensorManager.remapCoordinateSystem(instance.rotationMatrix, android.hardware.SensorManager
								.AXIS_MINUS_Y, android.hardware.SensorManager.AXIS_X, rotationMatrixOut);
						android.hardware.SensorManager.getOrientation(rotationMatrixOut, orientations);
					} else {
						android.hardware.SensorManager.getOrientation(instance.rotationMatrix, orientations);
					}
					sensorValue = (double) orientations[2];
					return sensorValue * RADIAN_TO_DEGREE_CONST * -1f;
				}
			case Y_INCLINATION:
				if (instance.useRotationVectorFallback) {
					float rawInclinationY;
					if ((rotate = rotateOrientation()) != 0) {
						rawInclinationY = RADIAN_TO_DEGREE_CONST * (float) (Math.acos(instance.accelerationXYZ[0] * rotate));
					} else {
						rawInclinationY = RADIAN_TO_DEGREE_CONST * (float) (Math.acos(instance.accelerationXYZ[1]));
					}
					float correctedInclinationY = 0;
					if (rawInclinationY >= 90 && rawInclinationY <= 180) {
						if (instance.signAccelerationZ > 0) {
							correctedInclinationY = -(rawInclinationY - 90);
						} else {
							correctedInclinationY = -(180 + (90 - rawInclinationY));
						}
					} else if (rawInclinationY >= 0 && rawInclinationY < 90) {
						if (instance.signAccelerationZ > 0) {
							correctedInclinationY = (90 - rawInclinationY);
						} else {
							correctedInclinationY = (90 + rawInclinationY);
						}
					}
					return (double) correctedInclinationY;
				} else {
					orientations = new float[3];
					android.hardware.SensorManager.getRotationMatrixFromVector(instance.rotationMatrix,
							instance.rotationVector);
					if (rotateOrientation() == 1) {
						android.hardware.SensorManager.remapCoordinateSystem(instance.rotationMatrix, android.hardware.SensorManager
								.AXIS_Y, android.hardware.SensorManager.AXIS_MINUS_X, rotationMatrixOut);
						android.hardware.SensorManager.getOrientation(rotationMatrixOut, orientations);
					} else if (rotateOrientation() == -1) {
						android.hardware.SensorManager.remapCoordinateSystem(instance.rotationMatrix, android.hardware.SensorManager
								.AXIS_MINUS_Y, android.hardware.SensorManager.AXIS_X, rotationMatrixOut);
						android.hardware.SensorManager.getOrientation(rotationMatrixOut, orientations);
					} else {
						android.hardware.SensorManager.getOrientation(instance.rotationMatrix, orientations);
					}

					float xInclinationUsedToExtendRangeOfRoll = orientations[2] * RADIAN_TO_DEGREE_CONST * -1f;

					sensorValue = (double) orientations[1];

					if (Math.abs(xInclinationUsedToExtendRangeOfRoll) <= 90f) {
						return sensorValue * RADIAN_TO_DEGREE_CONST * -1f;
					} else {
						float uncorrectedYInclination = sensorValue.floatValue() * RADIAN_TO_DEGREE_CONST * -1f;

						if (uncorrectedYInclination > 0f) {
							return (double) 180f - uncorrectedYInclination;
						} else {
							return (double) -180f - uncorrectedYInclination;
						}
					}
				}
			case FACE_DETECTED:
				return (double) instance.firstFaceDetected;
			case FACE_SIZE:
				return (double) instance.firstFaceSize;
			case FACE_X_POSITION:
				return getXAccordingToRotation(instance.firstFacePositionX,
						instance.firstFacePositionY);
			case FACE_Y_POSITION:
				return getYAccordingToRotation(instance.firstFacePositionX,
						instance.firstFacePositionY);
			case SECOND_FACE_DETECTED:
				return (double) instance.secondFaceDetected;
			case SECOND_FACE_SIZE:
				return (double) instance.secondFaceSize;
			case SECOND_FACE_X_POSITION:
				return getXAccordingToRotation(instance.secondFacePositionX,
						instance.secondFacePositionY);
			case SECOND_FACE_Y_POSITION:
				return getYAccordingToRotation(instance.secondFacePositionX,
						instance.secondFacePositionY);
			case NOSE_X:
				return getXAccordingToRotation(instance.noseX, instance.noseY);
			case NOSE_Y:
				return getYAccordingToRotation(instance.noseX, instance.noseY);
			case LEFT_EYE_INNER_X:
				return getXAccordingToRotation(instance.leftEyeInnerX, instance.leftEyeInnerY);
			case LEFT_EYE_INNER_Y:
				return getYAccordingToRotation(instance.leftEyeInnerX, instance.leftEyeInnerY);
			case LEFT_EYE_CENTER_X:
				return getXAccordingToRotation(instance.leftEyeCenterX, instance.leftEyeCenterY);
			case LEFT_EYE_CENTER_Y:
				return getYAccordingToRotation(instance.leftEyeCenterX, instance.leftEyeCenterY);
			case LEFT_EYE_OUTER_X:
				return getXAccordingToRotation(instance.leftEyeOuterX, instance.leftEyeOuterY);
			case LEFT_EYE_OUTER_Y:
				return getYAccordingToRotation(instance.leftEyeOuterX, instance.leftEyeOuterY);
			case RIGHT_EYE_INNER_X:
				return getXAccordingToRotation(instance.rightEyeInnerX, instance.rightEyeInnerY);
			case RIGHT_EYE_INNER_Y:
				return getYAccordingToRotation(instance.rightEyeInnerX, instance.rightEyeInnerY);
			case RIGHT_EYE_CENTER_X:
				return getXAccordingToRotation(instance.rightEyeCenterX, instance.rightEyeCenterY);
			case RIGHT_EYE_CENTER_Y:
				return getYAccordingToRotation(instance.rightEyeCenterX, instance.rightEyeCenterY);
			case RIGHT_EYE_OUTER_X:
				return getXAccordingToRotation(instance.rightEyeOuterX, instance.rightEyeOuterY);
			case RIGHT_EYE_OUTER_Y:
				return getYAccordingToRotation(instance.rightEyeOuterX, instance.rightEyeOuterY);
			case LEFT_EAR_X:
				return getXAccordingToRotation(instance.leftEarX, instance.leftEarY);
			case LEFT_EAR_Y:
				return getYAccordingToRotation(instance.leftEarX, instance.leftEarY);
			case RIGHT_EAR_X:
				return getXAccordingToRotation(instance.rightEarX, instance.rightEarY);
			case RIGHT_EAR_Y:
				return getYAccordingToRotation(instance.rightEarX, instance.rightEarY);
			case MOUTH_LEFT_CORNER_X:
				return getXAccordingToRotation(instance.mouthLeftCornerX, instance.mouthLeftCornerY);
			case MOUTH_LEFT_CORNER_Y:
				return getYAccordingToRotation(instance.mouthLeftCornerX, instance.mouthLeftCornerY);
			case MOUTH_RIGHT_CORNER_X:
				return getXAccordingToRotation(instance.mouthRightCornerX, instance.mouthRightCornerY);
			case MOUTH_RIGHT_CORNER_Y:
				return getYAccordingToRotation(instance.mouthRightCornerX, instance.mouthRightCornerY);
			case LEFT_SHOULDER_X:
				return getXAccordingToRotation(instance.leftShoulderX, instance.leftShoulderY);
			case LEFT_SHOULDER_Y:
				return getYAccordingToRotation(instance.leftShoulderX, instance.leftShoulderY);
			case RIGHT_SHOULDER_X:
				return getXAccordingToRotation(instance.rightShoulderX, instance.rightShoulderY);
			case RIGHT_SHOULDER_Y:
				return getYAccordingToRotation(instance.rightShoulderX, instance.rightShoulderY);
			case LEFT_ELBOW_X:
				return getXAccordingToRotation(instance.leftElbowX, instance.leftElbowY);
			case LEFT_ELBOW_Y:
				return getYAccordingToRotation(instance.leftElbowX, instance.leftElbowY);
			case RIGHT_ELBOW_X:
				return getXAccordingToRotation(instance.rightElbowX, instance.rightElbowY);
			case RIGHT_ELBOW_Y:
				return getYAccordingToRotation(instance.rightElbowX, instance.rightElbowY);
			case LEFT_WRIST_X:
				return getXAccordingToRotation(instance.leftWristX, instance.leftWristY);
			case LEFT_WRIST_Y:
				return getYAccordingToRotation(instance.leftWristX, instance.leftWristY);
			case RIGHT_WRIST_X:
				return getXAccordingToRotation(instance.rightWristX, instance.rightWristY);
			case RIGHT_WRIST_Y:
				return getYAccordingToRotation(instance.rightWristX, instance.rightWristY);
			case LEFT_PINKY_KNUCKLE_X:
				return getXAccordingToRotation(instance.leftPinkyKnuckleX, instance.leftPinkyKnuckleY);
			case LEFT_PINKY_KNUCKLE_Y:
				return getYAccordingToRotation(instance.leftPinkyKnuckleX, instance.leftPinkyKnuckleY);
			case RIGHT_PINKY_KNUCKLE_X:
				return getXAccordingToRotation(instance.rightPinkyKnuckleX, instance.rightPinkyKnuckleY);
			case RIGHT_PINKY_KNUCKLE_Y:
				return getYAccordingToRotation(instance.rightPinkyKnuckleX, instance.rightPinkyKnuckleY);
			case LEFT_INDEX_KNUCKLE_X:
				return getXAccordingToRotation(instance.leftIndexKnuckleX, instance.leftIndexKnuckleY);
			case LEFT_INDEX_KNUCKLE_Y:
				return getYAccordingToRotation(instance.leftIndexKnuckleX, instance.leftIndexKnuckleY);
			case RIGHT_INDEX_KNUCKLE_X:
				return getXAccordingToRotation(instance.rightIndexKnuckleX, instance.rightIndexKnuckleY);
			case RIGHT_INDEX_KNUCKLE_Y:
				return getYAccordingToRotation(instance.rightIndexKnuckleX, instance.rightIndexKnuckleY);
			case LEFT_THUMB_KNUCKLE_X:
				return getXAccordingToRotation(instance.leftThumbKnuckleX, instance.leftThumbKnuckleY);
			case LEFT_THUMB_KNUCKLE_Y:
				return getYAccordingToRotation(instance.leftThumbKnuckleX, instance.leftThumbKnuckleY);
			case RIGHT_THUMB_KNUCKLE_X:
				return getXAccordingToRotation(instance.rightThumbKnuckleX, instance.rightThumbKnuckleY);
			case RIGHT_THUMB_KNUCKLE_Y:
				return getYAccordingToRotation(instance.rightThumbKnuckleX, instance.rightThumbKnuckleY);
			case LEFT_HIP_X:
				return getXAccordingToRotation(instance.leftHipX, instance.leftHipY);
			case LEFT_HIP_Y:
				return getYAccordingToRotation(instance.leftHipX, instance.leftHipY);
			case RIGHT_HIP_X:
				return getXAccordingToRotation(instance.rightHipX, instance.rightHipY);
			case RIGHT_HIP_Y:
				return getYAccordingToRotation(instance.rightHipX, instance.rightHipY);
			case LEFT_KNEE_X:
				return getXAccordingToRotation(instance.leftKneeX, instance.leftKneeY);
			case LEFT_KNEE_Y:
				return getYAccordingToRotation(instance.leftKneeX, instance.leftKneeY);
			case RIGHT_KNEE_X:
				return getXAccordingToRotation(instance.rightKneeX, instance.rightKneeY);
			case RIGHT_KNEE_Y:
				return getYAccordingToRotation(instance.rightKneeX, instance.rightKneeY);
			case LEFT_ANKLE_X:
				return getXAccordingToRotation(instance.leftAnkleX, instance.leftAnkleY);
			case LEFT_ANKLE_Y:
				return getYAccordingToRotation(instance.leftAnkleX, instance.leftAnkleY);
			case RIGHT_ANKLE_X:
				return getXAccordingToRotation(instance.rightAnkleX, instance.rightAnkleY);
			case RIGHT_ANKLE_Y:
				return getYAccordingToRotation(instance.rightAnkleX, instance.rightAnkleY);
			case LEFT_HEEL_X:
				return getXAccordingToRotation(instance.leftHeelX, instance.leftHeelY);
			case LEFT_HEEL_Y:
				return getYAccordingToRotation(instance.leftHeelX, instance.leftHeelY);
			case RIGHT_HEEL_X:
				return getXAccordingToRotation(instance.rightHeelX, instance.rightHeelY);
			case RIGHT_HEEL_Y:
				return getYAccordingToRotation(instance.rightHeelX, instance.rightHeelY);
			case LEFT_FOOT_INDEX_X:
				return getXAccordingToRotation(instance.leftFootIndexX, instance.leftFootIndexY);
			case LEFT_FOOT_INDEX_Y:
				return getYAccordingToRotation(instance.leftFootIndexX, instance.leftFootIndexY);
			case RIGHT_FOOT_INDEX_X:
				return getXAccordingToRotation(instance.rightFootIndexX, instance.rightFootIndexY);
			case RIGHT_FOOT_INDEX_Y:
				return getYAccordingToRotation(instance.rightFootIndexX, instance.rightFootIndexY);
			case TEXT_FROM_CAMERA:
				return instance.textFromCamera;
			case TEXT_BLOCKS_NUMBER:
				return (double) instance.textBlocksNumber;
			case LOUDNESS:
				return (double) instance.loudness;
			case TIMER:
				return (SystemClock.uptimeMillis() - timerReferenceValue) / 1000d;
			case DATE_YEAR:
				return Double.valueOf(Calendar.getInstance().get(Calendar.YEAR));
			case DATE_MONTH:
				return Double.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1);
			case DATE_DAY:
				return Double.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
			case DATE_WEEKDAY:
				return startWeekWithMonday();
			case TIME_HOUR:
				return Double.valueOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
			case TIME_MINUTE:
				return Double.valueOf(Calendar.getInstance().get(Calendar.MINUTE));
			case TIME_SECOND:
				return Double.valueOf(Calendar.getInstance().get(Calendar.SECOND));

			case NXT_SENSOR_1:
			case NXT_SENSOR_2:
			case NXT_SENSOR_3:
			case NXT_SENSOR_4:

				LegoNXT nxt = btService.getDevice(BluetoothDevice.LEGO_NXT);
				if (nxt != null) {
					return Double.valueOf(nxt.getSensorValue(sensor));
				}
				break;

			case EV3_SENSOR_1:
			case EV3_SENSOR_2:
			case EV3_SENSOR_3:
			case EV3_SENSOR_4:
				LegoEV3 ev3 = btService.getDevice(BluetoothDevice.LEGO_EV3);
				if (ev3 != null) {
					return Double.valueOf(ev3.getSensorValue(sensor));
				}
				break;

			case PHIRO_BOTTOM_LEFT:
			case PHIRO_BOTTOM_RIGHT:
			case PHIRO_FRONT_LEFT:
			case PHIRO_FRONT_RIGHT:
			case PHIRO_SIDE_LEFT:
			case PHIRO_SIDE_RIGHT:
				Phiro phiro = btService.getDevice(BluetoothDevice.PHIRO);
				if (phiro != null) {
					return Double.valueOf(phiro.getSensorValue(sensor));
				}
				break;

			case GAMEPAD_A_PRESSED:
			case GAMEPAD_B_PRESSED:
			case GAMEPAD_DOWN_PRESSED:
			case GAMEPAD_LEFT_PRESSED:
			case GAMEPAD_RIGHT_PRESSED:
			case GAMEPAD_UP_PRESSED:
				return CastManager.getInstance().isButtonPressed(sensor) ? 1.0 : 0.0;

			case LAST_FINGER_INDEX:
				return Double.valueOf(TouchUtil.getLastTouchIndex());
			case FINGER_TOUCHED:
				return TouchUtil.isTouching();
			case FINGER_X:
				return Double.valueOf(TouchUtil.getX(TouchUtil.getLastTouchIndex()));
			case FINGER_Y:
				return Double.valueOf(TouchUtil.getY(TouchUtil.getLastTouchIndex()));
			case NUMBER_CURRENT_TOUCHES:
				return Double.valueOf(TouchUtil.getNumberOfCurrentTouches());

			case DRONE_BATTERY_STATUS:
			case DRONE_EMERGENCY_STATE:
			case DRONE_USB_REMAINING_TIME:
			case DRONE_NUM_FRAMES:
			case DRONE_RECORDING:
			case DRONE_FLYING:
			case DRONE_INITIALIZED:
			case DRONE_USB_ACTIVE:
			case DRONE_CAMERA_READY:
			case DRONE_RECORD_READY:
				return 0d;
			case NFC_TAG_MESSAGE:
				return String.valueOf(NfcHandler.getLastNfcTagMessage());
			case NFC_TAG_ID:
				return String.valueOf(NfcHandler.getLastNfcTagId());
			case SPEECH_RECOGNITION_LANGUAGE:
				return listeningLanguageSensor;
		}
		return 0d;
	}

	public static String getListeningLanguageSensor() {
		return listeningLanguageSensor;
	}

	public static void setListeningLanguageSensor(String listeningLanguageTag) {
		listeningLanguageSensor = listeningLanguageTag;
		Log.d(TAG, "listening language sensor changed to: " + listeningLanguageSensor);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	private static boolean isDeviceDefaultRotationLandscape() {
		int rotation = ((WindowManager) CatroidApplication.getAppContext().getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getRotation();
		Configuration config = CatroidApplication.getAppContext().getResources().getConfiguration();

		return config.orientation == Configuration.ORIENTATION_LANDSCAPE && (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
				|| config.orientation == Configuration.ORIENTATION_PORTRAIT && (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270);
	}

	private static int rotateOrientation() {
		if (ProjectManager.getInstance().isCurrentProjectLandscapeMode() ^ isDeviceDefaultRotationLandscape()) {
			return ProjectManager.getInstance().isCurrentProjectLandscapeMode() ? 1 : -1;
		}
		return 0;
	}

	private static double getXAccordingToRotation(
			float firstCoordinate,
			float secondCoordinate
	) {
		int rotate;
		if ((rotate = rotateOrientation()) != 0) {
			return ((-secondCoordinate) * rotate);
		} else {
			return firstCoordinate;
		}
	}

	private static double getYAccordingToRotation(
			float firstCoordinate,
			float secondCoordinate
	) {
		int rotate;
		if ((rotate = rotateOrientation()) != 0) {
			return ((firstCoordinate) * rotate);
		} else {
			return secondCoordinate;
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
			case Sensor.TYPE_MAGNETIC_FIELD:
				float[] tempMagneticFieldXYZ = event.values.clone();
				float[] tempInclinationMatrix = new float[9];
				android.hardware.SensorManager.getRotationMatrix(rotationMatrix, tempInclinationMatrix, accelerationXYZ,
						tempMagneticFieldXYZ);    //http://goo.gl/wo6QK5
				break;
			case Sensor.TYPE_ACCELEROMETER:
				accelerationXYZ = event.values.clone();
				if (useLinearAccelerationFallback) {
					determinePseudoLinearAcceleration(accelerationXYZ.clone());
				}
				double normOfG = Math.sqrt(accelerationXYZ[0] * accelerationXYZ[0]
						+ accelerationXYZ[1] * accelerationXYZ[1]
						+ accelerationXYZ[2] * accelerationXYZ[2]);
				accelerationXYZ[0] = (float) (accelerationXYZ[0] / normOfG);
				accelerationXYZ[1] = (float) (accelerationXYZ[1] / normOfG);
				accelerationXYZ[2] = (float) (accelerationXYZ[2] / normOfG);
				signAccelerationZ = Math.signum(event.values[2]);
				break;
			case Sensor.TYPE_LINEAR_ACCELERATION:
				linearAccelerationX = event.values[0];
				linearAccelerationY = event.values[1];
				linearAccelerationZ = event.values[2];
				break;
			case Sensor.TYPE_ROTATION_VECTOR:
				rotationVector[0] = event.values[0];
				rotationVector[1] = event.values[1];
				rotationVector[2] = event.values[2];
				break;
			default:
				Log.v(TAG, "Unhandled sensor type: " + event.sensor.getType());
		}
	}

	private void determinePseudoLinearAcceleration(float[] input) {
		float alpha = 0.8f;

		gravity[0] = alpha * gravity[0] + ((1 - alpha) * input[0]);
		gravity[1] = alpha * gravity[1] + ((1 - alpha) * input[1]);
		gravity[2] = alpha * gravity[2] + ((1 - alpha) * input[2]);

		linearAccelerationX = -1f * (input[0] - gravity[0]);
		linearAccelerationY = -1f * (input[1] - gravity[1]);
		linearAccelerationZ = -1f * (input[2] - gravity[2]);
	}

	@Override
	public void onCustomSensorChanged(SensorCustomEvent event) {
		switch (event.sensor) {
			case LOUDNESS:
				instance.loudness = event.values[0];
				break;
			case FACE_DETECTED:
				instance.firstFaceDetected = event.values[0];
				break;
			case FACE_SIZE:
				instance.firstFaceSize = event.values[0];
				break;
			case FACE_X_POSITION:
				instance.firstFacePositionX = event.values[0];
				break;
			case FACE_Y_POSITION:
				instance.firstFacePositionY = event.values[0];
				break;
			case SECOND_FACE_DETECTED:
				instance.secondFaceDetected = event.values[0];
				break;
			case SECOND_FACE_SIZE:
				instance.secondFaceSize = event.values[0];
				break;
			case SECOND_FACE_X_POSITION:
				instance.secondFacePositionX = event.values[0];
				break;
			case SECOND_FACE_Y_POSITION:
				instance.secondFacePositionY = event.values[0];
				break;
			case TEXT_BLOCKS_NUMBER:
				instance.textBlocksNumber = event.values[0];
				break;
			case TEXT_FROM_CAMERA:
				instance.textFromCamera = event.valuesString[0];
				break;
			case NOSE_X:
				instance.noseX = event.values[0];
				break;
			case NOSE_Y:
				instance.noseY = event.values[0];
				break;
			case LEFT_EYE_INNER_X:
				instance.leftEyeInnerX = event.values[0];
				break;
			case LEFT_EYE_INNER_Y:
				instance.leftEyeInnerY = event.values[0];
				break;
			case LEFT_EYE_CENTER_X:
				instance.leftEyeCenterX = event.values[0];
				break;
			case LEFT_EYE_CENTER_Y:
				instance.leftEyeCenterY = event.values[0];
				break;
			case LEFT_EYE_OUTER_X:
				instance.leftEyeOuterX = event.values[0];
				break;
			case LEFT_EYE_OUTER_Y:
				instance.leftEyeOuterY = event.values[0];
				break;
			case RIGHT_EYE_INNER_X:
				instance.rightEyeInnerX = event.values[0];
				break;
			case RIGHT_EYE_INNER_Y:
				instance.rightEyeInnerY = event.values[0];
				break;
			case RIGHT_EYE_CENTER_X:
				instance.rightEyeCenterX = event.values[0];
				break;
			case RIGHT_EYE_CENTER_Y:
				instance.rightEyeCenterY = event.values[0];
				break;
			case RIGHT_EYE_OUTER_X:
				instance.rightEyeOuterX = event.values[0];
				break;
			case RIGHT_EYE_OUTER_Y:
				instance.rightEyeOuterY = event.values[0];
				break;
			case LEFT_EAR_X:
				instance.leftEarX = event.values[0];
				break;
			case LEFT_EAR_Y:
				instance.leftEarY = event.values[0];
				break;
			case RIGHT_EAR_X:
				instance.rightEarX = event.values[0];
				break;
			case RIGHT_EAR_Y:
				instance.rightEarY = event.values[0];
				break;
			case MOUTH_LEFT_CORNER_X:
				instance.mouthLeftCornerX = event.values[0];
				break;
			case MOUTH_LEFT_CORNER_Y:
				instance.mouthLeftCornerY = event.values[0];
				break;
			case MOUTH_RIGHT_CORNER_X:
				instance.mouthRightCornerX = event.values[0];
				break;
			case MOUTH_RIGHT_CORNER_Y:
				instance.mouthRightCornerY = event.values[0];
				break;
			case LEFT_SHOULDER_X:
				instance.leftShoulderX = event.values[0];
				break;
			case LEFT_SHOULDER_Y:
				instance.leftShoulderY = event.values[0];
				break;
			case RIGHT_SHOULDER_X:
				instance.rightShoulderX = event.values[0];
				break;
			case RIGHT_SHOULDER_Y:
				instance.rightShoulderY = event.values[0];
				break;
			case LEFT_ELBOW_X:
				instance.leftElbowX = event.values[0];
				break;
			case LEFT_ELBOW_Y:
				instance.leftElbowY = event.values[0];
				break;
			case RIGHT_ELBOW_X:
				instance.rightElbowX = event.values[0];
				break;
			case RIGHT_ELBOW_Y:
				instance.rightElbowY = event.values[0];
				break;
			case LEFT_WRIST_X:
				instance.leftWristX = event.values[0];
				break;
			case LEFT_WRIST_Y:
				instance.leftWristY = event.values[0];
				break;
			case RIGHT_WRIST_X:
				instance.rightWristX = event.values[0];
				break;
			case RIGHT_WRIST_Y:
				instance.rightWristY = event.values[0];
				break;
			case LEFT_PINKY_KNUCKLE_X:
				instance.leftPinkyKnuckleX = event.values[0];
				break;
			case LEFT_PINKY_KNUCKLE_Y:
				instance.leftPinkyKnuckleY = event.values[0];
				break;
			case RIGHT_PINKY_KNUCKLE_X:
				instance.rightPinkyKnuckleX = event.values[0];
				break;
			case RIGHT_PINKY_KNUCKLE_Y:
				instance.rightPinkyKnuckleY = event.values[0];
				break;
			case LEFT_INDEX_KNUCKLE_X:
				instance.leftIndexKnuckleX = event.values[0];
				break;
			case LEFT_INDEX_KNUCKLE_Y:
				instance.leftIndexKnuckleY = event.values[0];
				break;
			case RIGHT_INDEX_KNUCKLE_X:
				instance.rightIndexKnuckleX = event.values[0];
				break;
			case RIGHT_INDEX_KNUCKLE_Y:
				instance.rightIndexKnuckleY = event.values[0];
				break;
			case LEFT_THUMB_KNUCKLE_X:
				instance.leftThumbKnuckleX = event.values[0];
				break;
			case LEFT_THUMB_KNUCKLE_Y:
				instance.leftThumbKnuckleY = event.values[0];
				break;
			case RIGHT_THUMB_KNUCKLE_X:
				instance.rightThumbKnuckleX = event.values[0];
				break;
			case RIGHT_THUMB_KNUCKLE_Y:
				instance.rightThumbKnuckleY = event.values[0];
				break;
			case LEFT_HIP_X:
				instance.leftHipX = event.values[0];
				break;
			case LEFT_HIP_Y:
				instance.leftHipY = event.values[0];
				break;
			case RIGHT_HIP_X:
				instance.rightHipX = event.values[0];
				break;
			case RIGHT_HIP_Y:
				instance.rightHipY = event.values[0];
				break;
			case LEFT_KNEE_X:
				instance.leftKneeX = event.values[0];
				break;
			case LEFT_KNEE_Y:
				instance.leftKneeY = event.values[0];
				break;
			case RIGHT_KNEE_X:
				instance.rightKneeX = event.values[0];
				break;
			case RIGHT_KNEE_Y:
				instance.rightKneeY = event.values[0];
				break;
			case LEFT_ANKLE_X:
				instance.leftAnkleX = event.values[0];
				break;
			case LEFT_ANKLE_Y:
				instance.leftAnkleY = event.values[0];
				break;
			case RIGHT_ANKLE_X:
				instance.rightAnkleX = event.values[0];
				break;
			case RIGHT_ANKLE_Y:
				instance.rightAnkleY = event.values[0];
				break;
			case LEFT_HEEL_X:
				instance.leftHeelX = event.values[0];
				break;
			case LEFT_HEEL_Y:
				instance.leftHeelY = event.values[0];
				break;
			case RIGHT_HEEL_X:
				instance.rightHeelX = event.values[0];
				break;
			case RIGHT_HEEL_Y:
				instance.rightHeelY = event.values[0];
				break;
			case LEFT_FOOT_INDEX_X:
				instance.leftFootIndexX = event.values[0];
				break;
			case LEFT_FOOT_INDEX_Y:
				instance.leftFootIndexY = event.values[0];
				break;
			case RIGHT_FOOT_INDEX_X:
				instance.rightFootIndexX = event.values[0];
				break;
			case RIGHT_FOOT_INDEX_Y:
				instance.rightFootIndexY = event.values[0];
				break;
			default:
				Log.v(TAG, "Unhandled sensor: " + event.sensor);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location == null) {
			return;
		}
		if (location.getProvider().equals(LocationManager.GPS_PROVIDER) || !isGpsConnected) {
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			locationAccuracy = location.getAccuracy();
			altitude = location.hasAltitude() ? location.getAltitude() : 0;
		}

		if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			lastLocationGpsMillis = SystemClock.elapsedRealtime();
			lastLocationGps = location;
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onGpsStatusChanged(int event) {
		switch (event) {
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				if (lastLocationGps != null) {
					isGpsConnected = (SystemClock.elapsedRealtime() - lastLocationGpsMillis) < 3000;
				}
				break;
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				isGpsConnected = true;
				break;
		}
	}

	public static void destroy() {
		if (instance == null) {
			return;
		}
		stopSensorListeners();
		instance = null;
	}
}
