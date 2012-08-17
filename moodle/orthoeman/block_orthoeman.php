<?php
class block_orthoeman extends block_base {
	public function init() {
		$this->title = get_string('orthoeman', 'block_orthoeman');
	}


	public function get_content() {
		if ($this->content !== null) {
			return $this->content;
		}

		$this->content         =  new stdClass;
		$this->content->text   = 'The content of our OrthoEMan? block!';
		$this->content->footer = 'Footer here...';

		return $this->content;
	}
}
?>
