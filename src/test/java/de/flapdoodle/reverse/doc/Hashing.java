/*
 * Copyright (C) 2016
 *   Michael Mosmann <michael@mosmann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.reverse.doc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashing {
	
	public static String hash(Path file) throws IOException, NoSuchAlgorithmException {
		MessageDigest instance = MessageDigest.getInstance("SHA-256");

		byte[] bytes = Files.readAllBytes(file);
		instance.update(bytes);

		byte[] hashAsByte = instance.digest();
		StringBuilder sb = new StringBuilder(hashAsByte.length * 2);
		for(byte b: hashAsByte) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
