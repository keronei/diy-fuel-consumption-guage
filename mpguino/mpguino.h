/* --- Constants --------------------------------------------- */

#define Sleep_bkl                           0x01
#define Sleep_lcd                           0x02

#if (CFG_SERIAL_TX == 1)
   #define myubbr (16000000/16/9600-1)
#endif

#if (TANK_IN_EEPROM_CFG == 1)
   #define eepBlkAddr_Tank                  0xA0
   #define eepBlkSize_Tank                     9
#endif

#define nil                         3999999999ul
 
#define guinosigold                    B10100101 
#define guinosig                       B11100111 


//Vehicle Interface Pins      
//#define NC                                   1
#define InjectorOpenPin                        2      
#define InjectorClosedPin                      3      
//LCD Pins, conf moved to lcd.h
//#define DIPin                                  4 // register select RS      
//#define EnablePin                              5       
//#define ContrastPin                            6      
//#define DB4Pin                                 7       
//#define DB5Pin                                 8       
//#define BrightnessPin                          9      
//#define NC                                  10
//#define NC                                  11
//#define DB6Pin                                12       
//#define DB7Pin                                13      
#define VSSPin                                14  // analog 0
#define temp1Pin                              1      
#define temp2Pin                              2
#define lbuttonPin                            17  // Left Button, on analog 3
#define mbuttonPin                            18  // Middle Button, on analog 4
#define rbuttonPin                            19  // Right Button, on analog 5

/* --- Button bitmasks --- */ 
#define vssBit                              0x01  //  pin14 is a bitmask 1 on port C
#define lbuttonBit                          0x08  //  pin17 is a bitmask 8 on port C
#define mbuttonBit                          0x10  //  pin18 is a bitmask 16 on port C
#define rbuttonBit                          0x20  //  pin19 is a bitmask 32 on port C

// start with the buttons in the right state      
#define buttonsUp   lbuttonBit + mbuttonBit + rbuttonBit

// how many times will we try and loop in a second     
#define loopsPerSecond                         2   

/* --- LCD line buffer size --- */
#define bufsize                               17

/* --- Enums ------------------------------------------------- */

enum displayTypes {dtText=0, dtBigChars, dtBarGraph};

/* --- Typedefs ---------------------------------------------- */

typedef void (* pFunc)(void);//type for display function pointers      

/* --- Macros ------------------------------------------------ */

#define LeftButtonPressed        (!(buttonState & lbuttonBit))
#define RightButtonPressed       (!(buttonState & rbuttonBit))
#define MiddleButtonPressed      (!(buttonState & mbuttonBit))

#define MIN(value1, value2)\
    (((value1) >= (value2)) ? (value2) : (value1))

#define MAX(value2, value1)\
    (((value1)>=(value2)) ? (value1) : (value2))

#define mylength(x) (sizeof x / sizeof *x)

#define looptime 1000000ul/loopsPerSecond /* 0.5 second */

#if (CFG_IDLE_MESSAGE == 1)
#define IdleDisplayRequested     (IDLE_DISPLAY_DELAY > 0)
#endif

/* --- Globals ----------------------------------------------- */

int CLOCK;
unsigned char DISPLAY_TYPE;
unsigned char SCREEN;      
unsigned char HOLD_DISPLAY; 
static char LCDBUF1[bufsize];
static char LCDBUF2[bufsize];

#if (CFG_IDLE_MESSAGE != 0)
signed char IDLE_DISPLAY_DELAY;
#endif

#if (CFG_FUELCUT_INDICATOR != 0)
unsigned char FCUT_POS;
  #if (CFG_FUELCUT_INDICATOR == 2)
  /* XXX: With the Newhaven LCD, there is no backslash (character 0x5C)...
   * the backslash was replaced with the Yen currency symbol.  Other LCDs
   * might have a proper backslash, so we'll leave this in... */
  char spinner[4] = {'|', '/', '-', '\\'};
  #elif (CFG_FUELCUT_INDICATOR == 3)
  char spinner[4] = {'O', 'o', '.', '.'};
  #endif
#endif

#if (BARGRAPH_DISPLAY_CFG == 1)
/* The mpg() function call in a Trip class returns an unsigned long.
 * This value can be divided by 1000 in order to get 'true' mpg.
 * This means that an unsigned short int will hold 0xFFFF, or 65535
 * (65.535 MPG).  Since 65.535 is potentially a realistic value, 
 * I propose dropping one decimal of precision in favor of a higher 
 * maximum --> 655.35 mpg. This way we can save 20 bytes of memory. */
unsigned short PERIODIC_HIST[10];
#if (CFG_UNITS == 2)
unsigned short BAR_MIN = 33;     //min 3 L/100km
unsigned short BAR_LIMIT = 9;  /* 3-11 L/100km (0.5 L/px) */
#define BARGRAPH_LABEL "Graph 9-33l/km" //max 16 chars
//unsigned short BAR_MIN = 300;     //min 3 L/100km
//unsigned short BAR_LIMIT = 700;  /* 3-7 L/100km (0.25 L/px) */
//#define BARGRAPH_LABEL "Graph 3-7L/100km" //max 16 chars
#else
unsigned short BAR_MIN = 1800; //min 18 mpg
unsigned short BAR_LIMIT = 5800;  /* 18-58 mpg (2.5 mpg/px) */
#define BARGRAPH_LABEL "Graph 18-58 MPG" //max 16 chars
#endif

#endif

#if (CFG_UNITS == 2)
#define STARTUP_TEXT "MPGuino v0.97m" //max 16 chars
#else
#define STARTUP_TEXT "MPGuino v0.97u" //max 16 chars
#endif

enum longparms {
  contrastIdx=0, 
  vssPulsesPerMileIdx, 
  microSecondsPerGallonIdx, 
  injPulsesPer2Revolutions, 
  currentTripResetTimeoutUSIdx, 
  tankSizeIdx, 
  injectorSettleTimeIdx, 
  weightIdx, 
  fuelCostIdx, 
  vsspause,
  injEdgeIdx,
  voltmeterOffsetIdx
};

#if(CFG_UNITS==2) // L/100km units
/* default values */
unsigned long parms[]={
   05ul,
   5099ul,
   81580794ul,
   2ul,
   120000000ul,
   40000ul,
   500ul,
   950ul,
   180000ul,
   2ul,
   0ul,
   1000ul
};

char *parmLabels[]={
   "Contrast",
   "VSS Pulses/Km", 
   "MicroSec/Litre",
   "Pulses/2 revs",
   "Timout(microSec)",
   "Tank Litre*1000",
   "Injector DelayuS",
   "Weight (Kg)",
   "Fuel KSH/L *1000",
   "VSS Delay ms",
   "InjType 0,1,2", //Injector trigger 0-dn, 1-up, 2-PeakHold injector
   "VoltageOffset mV"
};
#else // MPG units
/* default values */
unsigned long parms[]={
   85ul,
   5059ul, //8208ul,
   81506557ul, //500 000 000ul,
   2ul,
   120000000ul,
   40000ul,
   500ul,
   950ul,
   180000ul,
   2ul,
   0ul,
   1000ul
};

char *parmLabels[]={
   "Contrast",
   "VSS Pulses/Mile", 
   "MicroSec/Gallon",
   "Pulses/2 revs",
   "Timout(microSec)",
   "Tank Gal * 1000",
   "Injector DelayuS",
   "Weight (lbs)",
   "Fuel cost *1000",
   "VSS Delay ms",
   "InjType 0,1,2", //Injector trigger 0-dn, 1-up, 2-PeakHold injector
   "VoltageOffset mV"
};
#endif

//middle button cycles through these brightness settings      
unsigned char brightness[]={255,220,150,0};
//unsigned char brightness[]={255,248,230,200}; //for red screen MTC-16205D (otherwise its too bright and 7805 regulator gets hot)

/* --- Classes --------------------------------------------- */

class Trip{      
  public:

  unsigned long loopCount; //how long has this trip been running
  unsigned long injPulses; //rpm
  unsigned long injHiSec;// seconds the injector has been open
  unsigned long injHius;// microseconds, fractional part of the injectors open
  unsigned long injIdleHiSec;// seconds the injector has been open
  unsigned long injIdleHius;// microseconds, fractional part of the injectors open
  unsigned long vssPulses;//from the speedo
  unsigned long vssEOCPulses;//from the speedo
  unsigned long vssPulseLength; // only used by instant

  //these functions actually return in thousandths,       
  unsigned long miles();       
  unsigned long gallons();      
  unsigned long mpg();          //LSB = 0.001 MPG
  unsigned long mph(); 
  unsigned long time();         //mmm.ss      
  unsigned long fuelCost();         //fuel cost per trip   
  unsigned long eocMiles();     //how many "free" miles?        
  unsigned long idleGallons();  //how many gallons spent at 0 mph?        
  void update(Trip t);      
  void reset();      
  //Trip();      
};      
 
#if( DRAGRACE_DISPLAY_CFG )
class Drag {
public:
   unsigned long time_millis; // timer
   unsigned long time_400m_ms; // 400m time (ET)
   unsigned long vss_pulses; // distance counter
   unsigned long trap_speed; // 400m final speed
   unsigned long time_100kmh_ms; // 100km/h accelleration time
   unsigned long vss_400m; // how many pulses is 400m
   bool waiting_start; // TRUE if we are dragrace screen and made reset()
   bool running; //TRUE

   void reset();
   void start();
   void update();
   void finish();
   unsigned long time();
   unsigned long time100kmh();
   unsigned long trapspeed();
   unsigned long power();
/*
402m (1/4 mile) time and speed measure:
---
dragrace screen
- middle button resets old data and stopwatch is waiting movement (VSS signal)
- START: when we get VSS signal, then stopwatch is started 
- when speed is 100kmh we remember that time
- when 400m is done, stopwatch is stopped and speed is saved
*/
};
#endif
