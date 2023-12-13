The file j2s.core_5.0.x.jar is the dual-mode java2script transpiler.
It needs to be installed in the Eclipse dropins/ folder before
Eclipse is (re)started. It will be activated because this
project has a .j2sjmol configuration file in it. 

Note that this is .j2sjmol, not just .j2s (which activates the SwingJS
transpiler). The .j2sjmol file triggers the plugin to use the 
"legacy java2script 4.2 transpiler" for Jmol. (This transpiler is 
actively developed; it's just legacy in the sense that it uses a 
very complex early idea for class loading and method disambiguation.
SwingJS completely replaced that.)

The SwingJS transpiler is for the Jmol-SwingJS project and other 
SwingJS-based applications, such as Jmol-SwingJS, JSJalview,
and OSP-TrackerJS, and the roughly 800 Mathe-Prisma project applets.

j2s.core.jar contains both of these transpilers. All development
work on them should be done from a fork of either the 
bobhanson/javascript GitHub project or the java2script/java2script GitHub
project. (I have forked java2script/java2script as bobhanson/java2script, 
but I regularly push all my changes to java2script/java2script as well.)

Bob Hanson, November 12, 2023

