#define LcdCharHeightPix  8
#define DIPin             4 // register select RS  
#define EnablePin         5 
#define ContrastPin       6  
#define DB4Pin            7
#define DB5Pin            8
#define BrightnessPin     9
#define DB6Pin           12
#define DB7Pin           13



/* --- LCD Commands --- */
#define LCD_ClearDisplay                  0x01
#define LCD_ReturnHome                    0x02

#define LCD_EntryMode                     0x04
#define LCD_EntryMode_Increment           0x02

#define LCD_DisplayOnOffCtrl              0x08
#define LCD_DisplayOnOffCtrl_DispOn       0x04
#define LCD_DisplayOnOffCtrl_CursOn       0x02
#define LCD_DisplayOnOffCtrl_CursBlink    0x01

#define LCD_SetCGRAM                      0x40
#define LCD_SetDDRAM                      0x80
/* you can OR a memory address with each of the above */

/*Replace #include "WConstants.h"  //all things wiring / arduino
#if ARDUINO >= 100
 #include "Arduino.h"
#else
 #include "WProgram.h"
#endif
*/

//LCD prototype      
namespace LCD{      
  void gotoXY(unsigned char x, unsigned char y);      
  void print(char * string);      
  void init();      
  void writeCGRAM(char *newchars, unsigned char numnew);
  void tickleEnable();      
  void cmdWriteSet();      
  void LcdCommandWrite(unsigned char value);      
  void LcdDataWrite(unsigned char value);
  unsigned char pushNibble(unsigned char value);      
};      
