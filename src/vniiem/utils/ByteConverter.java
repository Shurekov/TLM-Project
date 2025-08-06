package vniiem.utils;

public class ByteConverter {
	
	private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		return bytesToHex(bytes, 0, bytes.length);
	}
	
	public static String bytesToHex(byte[] bytes, int offset, int len) {
		if (bytes == null || bytes.length == 0)
			return null;
		char[] hexChars = new char[len * 2];
		for (int j = offset; j < offset + len; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[(j - offset) * 2] = HEX_CHARS[v >>> 4];
			hexChars[(j - offset) * 2 + 1] = HEX_CHARS[v & 0x0F];
		}
		return new String(hexChars);
	}
}
