<?php
session_start();
$action = $_GET["action"]; // 1- transform xml to json

switch ($action)
{
    case "1":
        //$lessonid = $_GET["lessonid"];
        $xml = simplexml_load_file("XML/XMLFile1.xml");
		$displaydata = GetTemplateData($xml);
        //print_r($displaydata);
        echo json_encode($displaydata);
        break;

}

function GetTemplateData($data)
{
 	$a = array();
	$a["attributes"]["id"] = strval($data["id"]);
	$a["attributes"]["Title"] = strval($data["Title"]);
	$a["attributes"]["abstract"] = strval($data->abstract);
	$index = 0;
	foreach ($data->Page as $key => $value) {
		$a["Page"][$index]["attributes"]["Grade"] = strval($value["Grade"]);
		$a["Page"][$index]["attributes"]["Title"] = strval($value["Title"]);
		$windex = 0;
		foreach ($value->Widget as $wkey => $wvalue) {
			$widgetype = strval($wvalue["type"]);
			$a["Page"][$index]["Widget"][$windex]["type"] = $widgetype;
			switch ($widgetype) {
				case 'compleximage':
					$a["Page"][$index]["Widget"][$windex]["Image"] = GetDisplayComplexImg($wvalue->Image, $index.$windex);
					$a["Images"][] = array('id' => $index.$windex,'url'=> strval($wvalue->Image->ImageURI) );
					break;;
				case 'quiz':
				$a["Page"][$index]["Widget"][$windex]["Quiz"] = GetDisplayQuizImg($wvalue,$index.$windex);
					break;
				case 'text';
					$a["Page"][$index]["Widget"][$windex]["Text"] = GetDisplayText($wvalue->Text);
					
				break;
				
			}
			$windex++;	
		}
		$index++;
	}
	return $a;
}

function GetDisplayComplexImg($data,$id)
{
  $r = array();
  if (isset($data->ImageID))
  {
  	$r["ImageID"]=strval($data->ImageID);
	$r["LoadMethod"] = "ID";
  }
  else {
      $r["ImageURI"] = strval($data->ImageURI);
	  $r["LoadMethod"] = "URI";
  }
  
  $r["ShowRegions"] = strval($data["ShowRegions"]);
  $r["id"] = $id;
  $r["width"] = strval($data["width"]);
  $r["height"] = strval($data["height"]);
  $r["ImageType"] = (count(get_object_vars($data)) > 2) ? "complex" : "simple";
  return $r;
}

function GetDisplayQuizImg($data,$id)
{
	$r = array();
    $r["Question"] = strval($data->Question);
	$r["id"] = $id;
	$count = count($data->Answer);
	for ($i=0;$i<$count;$i++)
		$r["Answer"][$i] = strval($data->Answer[$i]);
    return $r;
}

function GetDisplayText($data)
{
	return strval($data);
}



?>