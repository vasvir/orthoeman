# ORTHO-eMAN

This is the orthoeman **Moodle** plugin.

While the plugin was developed initially under the ORTHO-eMAN European Project which was focused on orthopaedic and rehabilitation scopes, we believe that it is generic enough to be useful in the general case and not only for doctors.

The plugin offers the capability of media quizzes on annotated images via the supplied Authoring Tool.

The students can then take the prepared quizzes using the provided Display tool.

The plugin has 3 main parts:
- an Authoring Tool to design visual interacting courses
- a Display Tool for students in order to evaluate their knowledge and skills
- The main PHP Moodle plugin core that provides interoperability with Moodle

## Getting Started

While there are plans to register this plugin with Moodle so far this has not happened. So if you want to try it out and you are not afraid of some shell commands here you go.

* Clone this repository on your computer
* Assuming that you have access to the host of Moodle via ssh
** cd to plugin directory
** Edit the scripts/deploy.sh in order to match your setup
** Execute the scripts/deploy.sh to install/update the plugin into the Moodle directory.
* Moodle will notice that you have a new plugin and it will ask you to activate it.
* After activation you will be able to add an activity of type ORTHO-eMAN in your courses.
