Project 2011-1-RO1-LEO05-15321 (Contract LLP-LdV/ToI/2011/RO/008)\
A web-based e-training platform for Extended Human Motion Investigation
in Orthopedics ORTHO-eMAN

TEACHER

GUIDE

[]{#anchor}[]{#anchor-1}Introduction
------------------------------------

The OrthoEMan Project is a technology transfer project based on a
previous Leonardo project named e-Medi. The goal of the project is to
integrate orthopedic content with a presentation and an evaluation
aspect in order to be used in a e-Learning environment. To that end the
chosen e-Learning system is moodle
([http://www.moodle.org](http://www.moodle.org/)), and naturally the
OrthoEMan has been developed as a moodle plug-in.

The OrthoEMan plugin has 3 aspects

-   Authoring Tool
-   Display Tool
-   Moodle Administration

The aim of this document is to provide simple guidelines for all parties
interested in using the OrthoEMan plugin, including the authors
(teachers) and the examinees (students),

[]{#anchor-2}Software Requirements
----------------------------------

The OrthoEMan plugin and its modules have been developed with the newest
W3C standards in mind in order to provide a modern experience to all
faculty members including both the teacher and the students. Thus any
HTML5 compatible browser should be enough for working with the OrthoEMan
plugin. Furthermore the plugin has been explicitly tested with

-   Firefox 19
-   Chrome 23
-   Internet Explorer 10

It is strongly advised to keep the computer which will access to the
OrthoEMan facility in good shape with the latest updates applied.

[]{#anchor-3}Entering Moodle
----------------------------

In order to visit the OrthoEMan\'s project development moodle site you
have to startup your browser and type
<http://orthoeman.iit.demokritos.gr/moodle/> in the browser\'s location
(URL) bar[^1]. Your browser\'s window should look like the following
picture.

![Illustration 1: Moodle first
contact](.//Pictures/100002010000068A000003DCC3117247.png){width="16.692cm"
height="7.955cm"}

The screen estate is divided in 3 main columns. Let\'s name it left
panel, right panel and main area.The left panel is used mainly for
navigation while the right panel has mostly informational or context
specific actions. In the main area moodle displays the current
activity\'s content.

### []{#anchor-4}Getting Moodle Accounts

While it is possible to visit the moodle development site without
logging in you will not be able to see most of the interesting things.
Therefore it is imperative to log in in order to fully use the system.

In order to get a valid account please sent an e-mail to
[mailto:orthoeman-devel\@iit.demokritos.gr](mailto:orthoeman-devel@iit.demokritos.gr)
asking for a teacher account.

This is the typical screen you see after a successful login.

![Illustration 2: Moodle
Navigation](.//Pictures/10000000000006900000041AF48EBB5E.png){width="15.685cm"
height="9.802cm"}

### []{#anchor-5}Course Hierarchy

In moodle speak we have Course Categories which are groups of courses,
In each category it is possible to have multiple courses. Each course
may have one or more authors (editing teachers in moodle speak). Each
course is divided in time slots and in each time slot it is allowed to
have multiple activities. One such activity is the OrthoEMan activity
and it corresponds to one case (in OrthoEMan speak.). In order to depict
the above relationships graphically imagine the following tree.

-   Course Category
-   Course
-   Time Slot
-   Activity -- OrthoEMan activity (case)

Navigate to the course of interest and you should see something like
this.

![Illustration 3: Course
Outline](.//Pictures/100002010000068A000003DCE312E4E5.png){width="17cm"
height="10.033cm"}

In this example you can see a weekly outline with one OrthoEMan
activity. The outline of the course, meaning the way the time slots are
allocated, is entirely decision of the course creator. The course
creator depending on the admininistrator may be different than the
course author (editing teacher).

One can think of this situation where the faculty decides about the
format of the course (duration, exams, etc.) and the current teacher
provides the content.

![Illustration 4: Turn Editing On to edit an OrthoEMan
case](.//Pictures/100000000000048E0000012899FDD767.png){width="17cm"
height="4.314cm"}

If you are a teacher a button in the right upper corner will prompt you
to turn editing on. You have to press it in order to turn the editing
on. This will allow you to enter the **Authoring Tool**. After you turn
the editing on a series of icons should populate each activity in
course\'s main area.

You can add resources such as links, documents, images and HTML labels
with the first combobox at the right side. You can also add various
activities. One such activity is the OrthoEMan activity.

### []{#anchor-6}Creating an OrthoEMan activity

[]{#anchor-7}Let\' s create an OrthoEMan activity. Click on the right
combobox in the desired time slot like the picture below:

![Illustration 5: OrthoEMan case
creation](.//Pictures/10000000000001C40000018B15B76C7F.png){width="11.957cm"
height="10.326cm"}

After that step a title and a brief description will be requested in
order to fully qualify the lesson. The window\'s contents should look
like the picture below.

![Illustration 6: OrthoEMan case initial
configuration](.//Pictures/100002010000068A000003DCFD319BDA.png){width="17cm"
height="10.033cm"}

The author has to enter the following pieces of information.

-   **Title**: That is the title of the case. It should be short and
    descriptive. The title will be visible from the course outline.
-   **Description**: A more detailed description of the case. This text
    may include HTML formatting elements and it will be displayed in the
    first page of the **Display Tool**.
-   TimeOut: The amount of time a student is allowed to spend in a case.
    The Display Tool will prevent further access or answer submissions
    from the student after the timeout duration has been passed.
-   Cruize Mode: The Label has description \'\'Display Correct
    Answers\'\'. This checkbox instructs Display Tool to display the
    correct answer to all authorized students. This can be used for a
    grace period after the exams have finished.

When you are done configuring the case, press the button \'\'Save and
Return to Course\'\'. In order to be able to alter the configuration you
just entered from the Course Outline you have to click on the little
icon that depicts a hand holding a pencil (update).

In order to visit the **Authoring Tool** and actually edit the case you
have to again click the update icon.

[]{#anchor-8}Authoring Tool
---------------------------

### []{#anchor-9}Entering Authoring Tool

The second time you visit the case configuration page you encounter a
page similar to the picture but with one important difference. In the
main area, at the start of the page, there is a link.

By clicking the link **Authoring Tool** will start in a frame inside
moodle. If screen estate proves to be scarce we may consider of having a
way starting **Authoring Tool** in a new page. Note also, that the way
of **Authoring Tool** invocation may change in general e.g. the link may
become a button in the middle of the page etc.

### []{#anchor-10}First Contact

The **Authoring Tool** is a web based application that helps the author
to create or edit an OrthoEMan compatible case. In the figure below you
can see the opening screen for the program.

  --- ---------------------------------------------
  1   Toolbar
  2   Pages Container
  3   Page Area
  4   Media Container
  5   Non Media Container (Text, Quiz, RangeQuiz)
  --- ---------------------------------------------

Table 1: Authoring Tool

#### []{#anchor-11}Toolbar

The toolbar hosts 3 visible buttons. From left to right the buttons are
**Save**, **Preview** (left aligned), and **Send a bug report** (right
aligned). With the **Save** button one can save in the database the
case. A teacher may choose to quickly preview the case in order to get
an idea what students will face when they take the exam. When the
**Preview **button is clicked **Authoring Tool** will invoke the
**Display Tool** in a different tab (or window) or reloads it if it
already exists.

The button at the right side, with the warning sign and the envelop, is
for the user to inform the OrthoEMan plugin authors about an unintended
behavior of the program (namely a bug). See the picture below for an
illustration of the bug reporting dialog. When you are filing a bug
report try to be concise, short, and to the point. In the subject type
the problem type you experience. In the body of message make sure you
mention:

-   What are you trying to do (intention of the user)
-   What are you actually doing (series of events and user actions)
-   How the computer responds (erratic (buggy) behavior)
-   How the computer should respond (expected (correct) behavior)

Finally there is one hidden button aimed at advancing debugging users.
The button can be made visible by the **Moodle** administrator or by a
user\'s browser with debug facilities (such as firebug). The button
toggles a console with debug messages representing user events and
program internal state. The button is positioned left of the **Report a
bug** button.

#### []{#anchor-12}Pages Container

The pages container contains the pages (slides) the author creates.
There are 4 buttons. The **Add / Remove** button pair helps the author
to create new pages and remove unneeded ones. The **Up / Down** buttons
helps the author to properly position the current page with respect to
the other slides. The pages are identified by their title. The **Display
Tool** may not display the **page title**. Nevertheless a concise **page
title** is strongly advised to be entered in order to help author
organize the case and keep the overall overview. Note that the **page
title** inside the slide will be updated when the **Page Title** textbox
looses its input focus.

#### []{#anchor-13}Page Area

The page area has the following elements

-   Page Title: identifies the page and it is displayed in the Pages
    Container slide area for each page. It is strongly advised to enter
    a short descriptive page title that will organize the case flow.
    Note that the Display Tool may not display the Page Title text.
-   Page Type: a combobox that identifies the page type. See below for a
    discussion of the available page types.
-   Grade: The grade of the page. The sum of all pages will be
    normalized at the end anyway so it is possible to use any relative
    value without worrying about normalization issues.
-   Negative Grade: The punishment value that is subtracted from the
    positive grade for a wrong answer.

#### []{#anchor-14}Media Container

The media container is the place where the image or the video is
displayed. There is an upload button that initiates the upload
procedure. In the case of image only **PNG** and **JPEG** image formats
are allowed. In case of video the following video types are allowed
(**MPEG**, **MOV**, **AVI**,and **MP4**). Note that **AVI** is not a
video format itself, but a container format meaning it may include
different video and audio encoding formats such as **divx**, **xvid**,
**theora**, etc\... In order for the video to be visible in modern HTML5
browsers it has to be trans-coded to **mp4** and to **webm** formats.
This operation may take several minutes and it is being done during
video upload. For a 10 second video a 90 second upload and trans-coding
time may be required depending on the server load. Also the operation
may fail if the original video format is not understandable by the
**ffmpeg** which is used on the server to perform the trans-coding. In
such a case you will have to resubmit the video using an alternative
format.

#### []{#anchor-15}Non Media Container

The **Non Media Container** contains the following widgets

-   Text: A text area for theory text, or instructions for the image
    hotspots.
-   Quiz: A multiple choice quiz. The widget supports arbitrary number
    of possible questions and arbitrary number of correct questions.
-   Range Quiz: A quiz that accepts as correct any answer in the
    specified range.

### []{#anchor-16}Case structure

A **lesson** consists of a collection of **pages.** Currently there are
no limits in the number of pages a lesson can have. Each **page** has a
a **title** and two **items** that should be edited and populated with
the author\'s content. The left panel of the application is responsible
for the management of the pages. There are buttons for adding and
removing pages and buttons for page reordering. The content item can be
of the following type:

-   Text
-   Image
-   Video
-   Quiz
-   RangeQuiz

However not all item type combinations are valid. A page can only have
one the following item type combinations.

-   Image -- Text
-   Image -- Quiz
-   Image -- Range Quiz
-   Video -- Text
-   Video -- Quiz
-   Text -- Quiz

A **page **is characterized by its title and by its type (the
combination of item types)

#### []{#anchor-17}Image -- Text

The **Image -- Text** page type used for two types of pages:

-   Theory pages with informational areas pointed
-   Hotspot identification by the students as it is depicted in the
    picture below.

The hotspots are drawn with a orange pen while the informational areas
are drawn with blue pen. (consult the color map table below for
reference). The choice where a drawing will be hotspot or informational
is selected with the combobox in the middle of the tools as it is shown
above. The difference is that hotspots are expected to be found by the
students during the exam while the informational areas are shown to
exemplify aspects of the theory.

  --------------- --------
  Hotspot         Orange
  Informational   Blue
  Helper          Yellow
  Eraser          Red
  Other uses      Black
  --------------- --------

Table 2: Color Map Table

The image container sports several tools in order to help the author
properly annotate the image.

From left to right:

-   **1-1**: Removes all scaling. Every pixel of the image corresponds
    to one pixel of your viewing area.

-   **Zoom In**:. Zooms in by 20%.

-   **Zoom Out**: Zooms out by 20%.

-   **Zoom To Fit width**: Scales the image to fit in the width of your
    client area of your browser. This is the default behavior when an
    image is uploaded.

-   **Zoom To Target**: Requests from the user to draw a rectangle and
    then zooms to it.

-   **Drawing Type combobox**: Select if the next drawing area will be a
    hotspot or an informational area.

-   **Rectangle**: Draws a rectangle.

-   **Ellipse**: Draws an ellipse.

-   **Polygon**: Draws a polygon as a series of points. When the mouse
    hovers over the first point (within a range of 20 pixels) then a
    circle is drawn to indicate that the polygon will be closed.
    Although it is possible to create non convex polygons with this
    freehand drawing they should be avoided as it is possible to confuse
    the hotspot detection algorithm of the plugin.

-   **Line**: Draws a line. Lines are helper elements and they are
    painted with a yellow pen. Lines are not displayed in the **Display
    Tool. **If multiple lines are drawn and the mouse hovers over an
    intersection the automatic angle calculation tool kicks in and
    displays the angle in degrees.

-   **Crosshair Tool**: Draws a croshair tool. Again this is a helper
    tool and it is painted with a yellow color meaning it is not
    displayed in the **Display Tool**.

-   **Eraser**: Paints with red every drawing when mouse hovers near it.
    When in red a click remove the drawing from the image. In order to
    remove multiple drawings a repeated selection of the **eraser** tool
    is required.

-   **Image Editing Tools**: Allows for brightness, contrast and image
    inversion control in order for medical finding to become apparent.

-   **Show Regions checkbox**: Specifies if the hotspots will be
    displayed from the **Display Tool **during the exam after student\'s
    answer submission.

#### []{#anchor-18}Image -- Quiz

In the image below the **Image -- Quiz** combination is depicted. When
the quiz is selected the hotspot functionality is disabled. Existing
hotspots are converted to informational drawings. The **quiz** widget is
shown in the right side of the **Authoring Tool.** The quiz supports
arbitrary number of possible questions and arbitrary number of correct
questions. The widget supports addition and removal of questions but not
reordering of the questions.

#### []{#anchor-19}Image -- Range Quiz

The **Image -- Range Quiz** page type asks from the student to submit a
value. The authoring teacher specifies in the right side panel the range
of acceptable answers. Before entering the range the teacher should also
type in a descriptive question just above the range. Make sure that
units are properly specified in the question and in the entered region
since the student can only type raw numbers.

#### []{#anchor-20}Video -- Text

The **Video -- Text** page type can be used only for theory and not for
student\'s evaluation. The authoring teacher provides a video (be
patient during upload and trans-coding) and a text description
highlighting the relevant points.

#### []{#anchor-21}Video -- Quiz

The **Video -- Quiz** page type is like the **Image -- Quiz** where the
authoring teacher specifies a multiple choice for the student to answer.

#### []{#anchor-22}***Text*** -- Quiz

The **Text -- Quiz** page type is a classic non multimedia quiz where
the student can be examined in theory.

[^1]: Note that the moodle installation has to be transferred from NCSR
    Demokritos to University of Craiova in Romania.
