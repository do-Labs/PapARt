import fr.inria.papart.procam.*;
import fr.inria.papart.procam.camera.*;
import fr.inria.papart.procam.display.*;
import fr.inria.papart.drawingapp.*;
import fr.inria.papart.scanner.*;

import org.bytedeco.javacpp.*;
import org.reflections.*;
import TUIO.*;
import toxi.geom.*;

import fr.inria.guimodes.Mode;
import fr.inria.papart.scanner.GrayCode;


int downScale = 2;

int displayTime = 200;
int captureTime = 180;
int delay = 70;

int decodeValue = 120;


PImage imageOut;
GrayCode grayCode;
PImage[] grayCodesCaptures;
PImage projectorView = null;

Camera cameraTracking;
ProjectorDisplay projector;

int cameraX;
int cameraY;

int decodeType = 1;


PGraphics decodedImage;

void settings(){
    fullScreen(P3D);
}

PApplet mainApplet;

public void setup(){

    mainApplet = this;

    Papart papart = Papart.projection(this);
    initGui();
    cameraTracking = papart.getCameraTracking();
    cameraX = cameraTracking.width();
    cameraY = cameraTracking.height();

    // Papart papart = Papart.projectionOnly(this);
    // initGui();
    // cameraX = 1920;
    // cameraY = 1080;
    // cameraTracking  = CameraFactory.createCamera(Camera.Type.FLY_CAPTURE, "0");
    // cameraTracking.setParent(this);
    // cameraTracking.setSize(cameraX, cameraY);
    // ((CameraFlyCapture) cameraTracking).setBayerDecode(true);
    // cameraTracking.start();
    // cameraTracking.setThread();


    decodedImage = createGraphics(cameraX, cameraY);

    projector = (ProjectorDisplay) papart.getDisplay();
    projector.manualMode();

    imageOut = createImage(cameraX, cameraY, RGB);

    frameRate(100);
}




PImage cameraImage;


private boolean getCameraImage(){
    cameraImage = cameraTracking.getPImage();
    return cameraImage != null;
}


void draw(){
    background(0);


    if(!getCameraImage()){
	return;
    }

    // video... (to check if it is the correct camera)
    drawWait();

    // capture...
    drawCode();

    // decoded !
    drawResult();
}

void drawWait(){
    if(Mode.is("wait")){
	image(cameraImage, 0, 0, width, height);
    }
}


void drawCode(){
    if(Mode.is("code")){
	updateCodes();

	// Display the gray code to capture
	// Other possibility.
	// 	image(grayCodes[code], 0, 0, frameSizeX, frameSizeY);

	grayCode.display((PGraphicsOpenGL) this.g, code);
	tryCaptureImage();

	if(allCodesCaptured()){
	    Mode.set("result");
	    decodeBang.show();
	}

    }
}


PVector imVisu = new PVector(400, 300);

void drawResult(){
    if(Mode.is("result")){

	if(!grayCode.isDecoded())
	    decode();

	updateCodes();

	image(decodedImage, 0, 0, cameraX / 2f, cameraY /2f);

	// Draw each captured image bone by one.
	image(grayCodesCaptures[code], cameraX, 0,
	      (int) imVisu.x, (int) imVisu.y);


	// Decode and show the decoded...
	PImage decodedIm = grayCode.getImageDecoded(code, decodeType, decodeValue);

	image(decodedIm, cameraX /2f, (int) imVisu.y,
	      (int) imVisu.x, (int) imVisu.y);

	image(projectorView,
	      0, cameraY/2,
	      width / 3,
	      height / 3);


    }
}

void decode(){

    println("Decode " + decodeType);
    println("value " + decodeValue);

    grayCode.decode(decodeType,decodeValue);

    drawDecoded();
    saveScanBang.show();
    //    projectorView = grayCode.getProjectorImage();
    projectorView = grayCode.getProjectorImageScaled(downScale);

    projectorView.save("ProjView.bmp");
    System.gc();
}




void tryCaptureImage(){
    if(captureOK()){

    	if(grayCodesCaptures[codeProjected] == null){

	    setNextCaptureTime();

	    // Create an image
	    PImage im = cameraTracking.getPImageCopy();
	    addCapturedImage(im);

	    if(codeProjected == nbCodes){
		grayCode.setRefImage(im);
	    }
    	}
    }
}

void addCapturedImage(PImage im){
    grayCode.addCapture(im, codeProjected);
    grayCodesCaptures[codeProjected] = im;
    nbCaptured++;
    println("Captured code: " + code + ", total: " + nbCaptured);
}


void startCapture(){
    Mode.set("code");

    grayCode = new GrayCode(this, width, height, downScale);
    grayCode.setBlackWhiteColors(0, 180);

    nbCodes = grayCode.nbCodes();
    grayCodesCaptures = new PImage[nbCodes];

    code = 0;
    codeProjected = 0;
    nbCaptured = 0;
    startTime = millis();
    nextCapture = startTime + captureTime;
    decodeBang.hide();
    saveScanBang.hide();
}


boolean test = false;

void keyPressed() {

  if(key == 't')
    test = !test;

}


void checkStart(){
    if(Mode.is("wait") || Mode.is("result")){
	startCapture();
    }
}


// Offscreen drawing.

void drawDecoded(){
    int[] decodedX = grayCode.decodedX();
    int[] decodedY = grayCode.decodedY();
    boolean[] mask = grayCode.mask();

    decodedImage.beginDraw();
    decodedImage.background(0);
    decodedImage.colorMode(RGB, width, height, 255);
    for(int y = 0 ; y < cameraY; y+= 1) {
	for(int x = 0; x < cameraX; x+= 1) {
	    int offset = x + y* cameraX;
	    if(!mask[offset]){
		decodedImage.stroke(255, 255, 255);
		decodedImage.stroke((int) random(255));
		decodedImage.point(x,y);
		continue;
	    }
	    decodedImage.stroke(decodedX[offset], decodedY[offset], 100);
	    decodedImage.point(x,y);
        }
    }
    decodedImage.endDraw();
}
