# LEQA: Latency Estimator for Quantum Algorithms

## Description
LEQA is a fast latency estimation tool for evaluating the performance of a quantum algorithm mapped to a quantum fabric. The actual quantum algorithm latency can be computed by performing detailed scheduling, placement and routing of the quantum instructions and qubits in a quantum operation dependency graph on a quantum circuit fabric. This is, however, a very expensive proposition that requires large amounts of processing time. Instead, LEQA, which is based on computing the neighborhood population counts of qubits, can produce estimates of the circuit latency with good accuracy (i.e., an average of less than 3% error) with up to two orders of magnitude speedup for mid-size benchmarks. This speedup is expected to increase super-linearly as a function of circuit size (operation count).


## Directories & Files Structure
```
LEQA
|-- sample_inputs
    |-- tfc -> Benchmarks taken from http://webhome.cs.uvic.ca/~dmaslov in TFC format (Toffoli, Fredkin, CNOT)
    |-- qasm -> TFC benchmarks converted to QASM format with fault-tolerant gate set (H, T, T_dagger, X, Y, Z, CNOT)
    |-- fabric.xml -> IonTrap fabric description
    `-- tech.xml -> Physical parameters for the Ion-Trap technology
|-- src
    |-- edu -> Java source code directory
    |-- libs
        |-- commons-cli-1.2.jar -> Appache Commons CLI library
        |-- commons-lang3-3.3.2.jar -> Apache Commons Lang library
        |-- jar-in-jar-loader.zip -> Jar loader file taken from Eclipse.
        |-- javacc.jar -> Java Compiler Compiler (JavaCC)
        `-- jgrapht-core-0.9.0.jar -> JGraphT library.
|-- build.xml -> Ant build file
|-- leqa.pdf -> LEQA paper published in DAC 2013
|-- leqa-prebuilt.jar -> Pre-built version of LEQA
|-- LICENSE -> License file
`-- README -> This readme file
```

## Requirements
1. [Ant 1.7](http://ant.apache.org) or higher
2. [Oracle Java 7-JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or higher  

## Preinstall
Make sure that all the requirements are already installed. The following environmental variable should be set before the installation/running of the program.
* `JAVA_HOME` should point where `java` and `javac` binary files are located.

## Compile
**Method 1:** Run the following command in the root directory of the project to build LEQA:
```
ant
```

This command will clean the built files:
```
ant clean
```

**Method 2:** You may use Eclipse to import source files as explained next.
- Select `File->Import`.
- Select `General->Existing Projects` into `Workspace` and choose `Next`.
- In the root directory, point to the location of Java source files.
- Select `Finish`.
- Make sure `JavaCC` plugin is installed in Eclipse. It can be obtained from [here](http://eclipse-javacc.sourceforge.net).

**Note:** We have included a pre-built version of LEQA called `leqa-prebuilt.jar`. It is recommended to use it if you do not want to modify LEQA.


## Run
Run the following command to perform the scheduling and placement of quantum logical instructions, and routing of logical qubits.
```
java -jar leqa.jar
```

LEQA options are listed below:
```
usage: leqa [-d] -f <file> [-h] -i <file> [-j] -s <num> -t <file> [-v]
LEQA estimates the latency of a given QASM/TFC mapped to a given PMD fabric.
 -d,--debug               Print debugging info
 -f,--fabric <file>       Fabric specification file
 -h,--help                Print this help menu
 -i,--input <file>        QASM/TFC input file (QASM is preferred)
 -j,--skip                Skip invocation of QSPR
 -s,--speed <num>         Qubit movement speed
 -t,--technology <file>   Technology file
 -v,--verbose             Verbosely prints the quantum operations
```

## Example
Getting the latency estimation for the 8-bit adder circuit described in QASM:
```
$ java -jar leqa.jar -f sample_inputs/fabric.xml -s 0.001 -t sample_inputs/tech.xml -i sample_inputs/qasm/8bitadder.qasm
```

**Note:** You can also provide quantum circuits described in TFC format to LEQA. LEQA first converts them to QASM and then performs the rest of its job. Note that this conversion is very simplistic and not optimal.

### Sample Outputs
The provided tool is fully tested on a server machine with the following specification:
 - OS: Debian Wheezy (Debian 7) AMD64 edition
 - CPU: Intel Core i7-3770 CPU @ 3.40GHz
 - Memory: 8 GB

**Note:** We have tested LEQA in Windows 7 and it worked flawlessly.

Output:
```
           Results            
------------------------------
Estimated value:       1666927
Actual value:          1616750
Error:                 3.10%
------------------------------
Parsing overhead:      0.115s
LEQA runtime:          0.047s
QSPR time:             0.323s
Speed up:              6.87
```

## Developers
* [Mohammad Javad Dousti](<dousti@usc.edu>)
* [Massoud Pedram](<pedram@usc.edu>)

## Questions or Bugs?
You may contact [Mohammad Javad Dousti](<dousti@usc.edu>) for any questions you may have or bugs that you find.

## License
Please refer to the [LICENSE](LICENSE) file.
