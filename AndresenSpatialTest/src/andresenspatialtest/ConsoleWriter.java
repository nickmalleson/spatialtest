/*This file is part of SpatialTest.

SpatialTest is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SpatialTest is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SpatialTest.  If not, see <http://www.gnu.org/licenses/>.*/


package andresenspatialtest;

/**
 * Simple interface that allows the algorithm to write to the GUI's console,
 * specifying whether the text is an error or not.
 * @author Nick Malleson
 */
public interface ConsoleWriter {

   /**
    * Write the text to the console.
    * @param text The text to write.
    * @param error Whether the text is an error (true) or normal (false).
    */
   void writeToConsole(String text, boolean error);

   void writeToConsole(StackTraceElement[] e);

}
