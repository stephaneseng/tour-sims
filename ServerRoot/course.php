<?php

// Establish the connection to the database
$connection = pg_connect("host=localhost port=5432 dbname=toursims user=toursims password=") or die('Could not connect: '.pg_last_error());

// Define and perform the SQL query
switch ($_REQUEST['action']) {
	case "get_courses":
		// Return a list of all the courses
		// Test: http://toursims.free.fr/course.php?action=get_courses
		$query = "
		SELECT
			c.course_id,
			c.name,
			c.description,
			c.difficulty,
			c.file,
			c.\"timestamp\",
			AVG(r.rating)
		FROM
			course AS c
				LEFT OUTER JOIN
					rating AS r
				ON
					c.course_id = r.course_id
		GROUP BY
			c.course_id;
		";
		$result = pg_query($query) or die('Query failed: '.pg_last_error());
		
		break;
	case "get_city_courses":
		// Return a list of all the courses from a specified city
		// Test: http://toursims.free.fr/course.php?action=get_city_courses&city_id=1
		$query = "
		SELECT
			c.course_id,
			c.name,
			c.description,
			c.difficulty,
			c.file,
			c.\"timestamp\",
			AVG(r.rating)
		FROM
			course AS c
				LEFT OUTER JOIN
					rating AS r
				ON
					c.course_id = r.course_id
		WHERE
			c.city_id = ".$_REQUEST['city_id']."
		GROUP BY
			c.course_id;
		";
		$result = pg_query($query) or die('Query failed: '.pg_last_error());
		
		break;
	default:
		// The requested action is invalid
		die('Invalid action');
		
		break;
}

// Display the results in JSON
include_once('JSON.php');
$json = new Services_JSON();
header('Content-Type: text/javascript');
$rows = array();
while($r = pg_fetch_assoc($result)) {
    $rows[] = $r;
}
print $json->encode($rows);

// Free the resultset
pg_free_result($result);

// Close the connection
pg_close($connection);

?>