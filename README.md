# ORTHO-eMAN
This is the orthoeman **Moodle** plugin.

The creation of this plugin was funded by a european project named
ORTHO-eMAN. Here are the relevant details:
>Project 2011-1-RO1-LEO05-15321 (Contract LLP-LdV/ToI/2011/RO/008)\
>A web-based e-training platform for Extended Human Motion Investigation
>in Orthopedics ORTHO-eMAN.

While the plugin was developed initially under the ORTHO-eMAN European Project which was focused on orthopaedic and rehabilitation scopes, we believe that it is generic enough to be useful in a broader scope case and not only for medical doctors.

The plugin offers the capability to create media quizzes on annotated images via the supplied Authoring Tool.

The students can then take the prepared quizzes using the provided Display tool.

If you want to take a sneak peak on what a student sees during the exam you can watch a video here.
[![Student Exam Environment. Click to start video.](https://i.vimeocdn.com/video/975966644_640.jpg "Student Exam. Click to start video.")](https://vimeo.com/437823452 "Student Exam Environment. Click to start video.")

The video is available here: [https://vimeo.com/437823452](https://vimeo.com/437823452)

Step by step instructions are available in [Display Tool Documentation](https://github.com/vasvir/orthoeman/blob/master/Display/doc/orthoeman-student-guide.md)

Quizzes have been created with the Authoring Tool. Here is a screenshot of what the Authoring Tool looks like.
![Authoring Tool](https://raw.githubusercontent.com/vasvir/orthoeman/master/AuthoringTool/doc/image_text_hotspot.png "Authoring Tool")

Step by step instructions are available in [Authoring Tool Documentation](https://github.com/vasvir/orthoeman/blob/master/AuthoringTool/doc/orthoeman-teacher-guide.md)

## Getting Started
While there are plans to register this plugin with Moodle so far this has not happened. So if you want to try it out and you are not afraid of some shell commands here you go.

**Note:** Do not mindlessly clone the repository inside the moodle directory. This will not work because a difference structure is expected during deployment. Instead try these steps:

* Clone this repository on your computer. Location does not play any role as long as it is not the moodle plugins destination folder.

```
git clone https://github.com/vasvir/orthoeman.git
```

Assuming that you have access to the host of Moodle via ssh

* cd to plugin directory of the newly cloned orthoeman repository

```
    cd orthoeman
```

* Edit the scripts/deploy.sh in order to match your setup

```
    vi scripts/deploy.sh
```

* Execute the scripts/deploy.sh to install/update the plugin into the Moodle directory.

```
    ./scripts/deploy.sh
```

* Moodle will notice that you have a new plugin and it will ask you to activate it.
* After activation you will be able to add an activity of type ORTHO-eMAN in your courses.

## Architecture
The plugin has 3 main parts:
1. Authoring Tool to design visual interacting courses
1. Display Tool for students in order to evaluate their knowledge and skills
1. Moodle plugin backend core that provides interoperability with Moodle

### Authoring Tool
The Authoring Tool is a GWT project with an eclipse project file in case you would like to resume development.

The list of 3r party dependecies is:
* GWT elemental libraries

### Display Tool
The Display tool is an Aptana powered eclipse project. It uses HTML5 Boilerplate project template and some PHP.

The list of 3r party dependecies is:
* HTML5 Boilerplate
* FirePHP
* jquery
* jquery.countdown
* jquery.pnotify
* jquery.prettyLoader
* jquery-ui
* jsrender
* kinetic
* modernizr
* myimagetools
* pixastic
* Queue
* spincontrol
* turn

### Moodle plugin backend core
The plugin backend consists of the following files.

* lib.php
* version.php
* view.php
* mod_form.php
* locallib.php
* index.php
* backup.php
* restore.php

The orthoeman plugin exposes an API for the Authoring and Display Tool to use. The following files implement this API.
* get_timeleft.php
* submit_grade.php
* report_bug.php
* get_details.php
* get_answers.php
* put_answer.php
* delete_answers.php
* get_resource.php
* put_resource.php

The list of 3r party dependecies is:
* byteserve.php
