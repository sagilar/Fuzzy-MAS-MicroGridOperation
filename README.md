# Fuzzy-MAS-MicroGridOperation
This repository contains a Java project which implements a Fuzzy-based Multi-Agent System to control the operation of a microgrid based on energy market dynamics.  
The publication of this work can be found in https://doi.org/10.1007/978-3-319-94779-2_26.  
The Java project launchs an application which can interact with Matlab and SQL databases, I tested it with POSTGRESQL and SQLite3. It also has a Graphical User Interface.  
The information to process about energy market dynamics is obtained from https://hourlypricing.comed.com/ and its API https://hourlypricing.comed.com/api?type=currenthouraverage&format=json.  

## About the Code
The Java project has 6 packages:
- agentes: the agents package.
- interfaz: the GUI code.
- json: a library to treat with json structures.
- ontology: the classes in which the application and the agents base to share information one another.
- paquetedbs: the package of the classes to interface the application with the databases.
- var: the classes to create the chart in the GUI.

## Remarks
If you use this project as a base for your project, please do the respective referenciation.  
**Most of the code and interfaces are in Spanish. Please contact me for help.**
