# OpenAI-API
This tool allows you to interact with the OpenAI API to send queries based on the contents of a document and receive responses. The program reads a local document, combines it with a user-defined query, and sends it to the OpenAI API to generate a response.

## Prerequisites

- **Java JDK**
- **OpenAI API key**
- **Canvas API key**
  

## Installation

1. **Install Dependencies**

    Make sure you have the latest JDK file installed and be able to run the program on your IDE before you compile it on Command Prompt

2. **Proper imports stored in src folder** 

    Json https://search.maven.org/remotecontent?filepath=org/json/json/20240303/json-20240303.jar

    Apache.tika https://dlcdn.apache.org/tika/3.0.0/tika-app-3.0.0.jar
    
   
4. **API Key Setup**

   Ensure you have your OpenAI API and Canvas API key stored as an environment variable:

   Check that you have the correct keys in your environment by running this:
   ```Command Prompt
   echo %YOUR_OPENAI_API_KEY%
   echo %YOUR_CANVAS_API_KEY%
   ```

## Usage

Run the program by specifying the path to your src folder with the needed imports

```Command Prompt
   cd C:\path\to\src\folder
```

For example:

```Command Prompt
   cd C:\Users\poncema4\OneDrive - Seton Hall University\FALL 2024\CSAS2123AA\IntelliJWorkSpace\Canvas API Project\src
```

Then...

To be able to just write "answer_discussion" to run your program open notepad or any text editor and write

```Notepad
@echo off
REM Set the classpath with your dependencies
set CLASSPATH=.;json-20230227.jar;apache-tika-1.28.5.jar
java LLM
```

This should be converted to a bat file now

Now you must compile the java files in order to be able to run the program properly

```Command Prompt
javac -cp ".;json-java 1.jar;tika-app-2.9.2.jar" FIRST_FILE_NAME.java SECOND_FILE_NAME.java ...if more
```

For example

```Command Prompt
javac -cp ".;json-java 1.jar;tika-app-2.9.2.jar" CanvasAPI.java LLM.java
```

Lastly, if you were successful, Command Prompt should say...

"Note: Annotation processing is enabled because one or more processors were found
on the class path. A future release of javac may disable annotation processing
unless at least one processor is specified by name (-processor), or a search
path is specified (--processor-path, --processor-module-path), or annotation
processing is enabled explicitly (-proc:only, -proc:full).
Use -Xlint:-options to suppress this message.
Use -proc:none to disable annotation processing.
Note: CanvasAPI.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details."

Check you src folder now and you should have your file names in .class format, this is how you also know that you successfully compiled your java files
(You only need to compile your java files once) -> If you make any changes, compile your files again to avoid any errors

## Run Program

```Command Prompt
answer_discussion
```

After you run the program you will get the prompt of your latest discussion post from your selected class from Canvas and a brief summary of the PDF that is
attached in that discussion post and will print out the response OpenAI generates.

"Response has been uploaded to Canvas!" <- Your reply has been posted, Enjoy! :) 

## Notes

- **API Key**: The program checks for the `OPENAI_API_KEY` environment variable. If it’s not set, the program will exit with an error.
