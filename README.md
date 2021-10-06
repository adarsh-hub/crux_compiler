# Compilers Project
### Adarsh B. Shankar

# Crux Compiler
Compiler for crux language ( a subset of C/C++ ) being done as part of the course project in CS142A(Compiler Design).

## Structure
This project is done in a series of milestones.
Structure of each milestone :
 - Doc: Contains problem statement and help regarding running the code.
 - Src: Contains the source code
 - Test: Contains test cases.

The final and complete project is in milestone 4.

## Intermidiate files:
- code.crux: Contains 3AC representation of code
- sym.dump: Contains symbol table
- m.s: Contains assembly code in X86 32bit, AT&T format
- m.out: Binary file for the compiled code


## SymbolTable : 
- This is a list of symbol Tables; One for global and one each for functions, block scopes and classes
- Each local and temp variables contain a base address, offset and size
- All entries for activation record are in function entry of symbol table

## Features :
### Basic features:
- Native data types: Int, Char, Float
- Variables and Expressions
- Conditional: if, if-else, switch-case
- Loops: for, while, do-while
- Break, Continue
- Arrays: Single and multidimensional
- Input,output
- Functions, recursion
- User-defined types (struct, class)
- Pointers: Single and multilevel
- Simple library functions
- Arithmetic operators
- Logical and bitwise operators

### Additional features
-   Function overloading
-   Class and its functions
-   Auto-type inference
-   Dynamic memory allocation: new, delete
-   Basic nested function(Can’t access variables of parent functions)
-   Class as a function parameter, Class Assignment.
-   An array as a function parameter
-   Global variables
