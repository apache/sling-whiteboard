# Declarative Dynamic Resource Sample 2

This is more or less the same as **Sample** but it is not added to the project so it will
not be built and installed during the build of DDR.
The Goal of this project is to showcase the dynamic nature of the DDR registration so that a
user can register new DDRs even when Sling is started and DDR project is installed.

## Design

These are the components of the Sample 2:

* **/apps/ddr-after/components**: the DDR target folder which is empty
* **/apps/ddr-static2**: the base component **button** and **text**
* **/conf/ddr-after/settings/dynamic**: the source DDR components that points to the DDR target of **/apps/ddr-after/components and contains an updated version of Button and Text
* **/content/ddr-after**: content that show the usage of enhanced Button and Text

## Review

This is the same as Sample2. After the installation you will find DDRs in /apps/ddr-after/components
as **button2** and **text2**.
Also opening the component under **/ddr-after/button.html** or **/ddr-after/text.html** you will
a page that will show the actual target found in **/ddr-static2**.

Andreas Schaefer: 3/28/2021