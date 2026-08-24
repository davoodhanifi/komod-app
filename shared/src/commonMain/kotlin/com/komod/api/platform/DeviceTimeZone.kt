package com.komod.api.platform

// Read fresh on every call (never cached in a val) so it reflects travel or DST changes
// without needing app restart. Always an IANA identifier (e.g. "Europe/Amsterdam"), never a
// fixed UTC offset. Returns null if the device timezone can't be determined; callers should
// omit timeZoneId entirely in that case so the backend falls back to UTC.
expect fun getDeviceTimeZoneId(): String?
