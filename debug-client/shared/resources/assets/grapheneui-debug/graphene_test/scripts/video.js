const videos = [
	{
		label: "WebM VP9",
		element: document.getElementById("webm-video"),
		status: document.getElementById("webm-status"),
		url: "../media/video-test-vp9.webm",
	},
	{
		label: "MP4 H.264",
		element: document.getElementById("mp4-video"),
		status: document.getElementById("mp4-status"),
		url: "../media/video-test-h264.mp4",
	},
];

const mediaErrorNames = {
	1: "MEDIA_ERR_ABORTED",
	2: "MEDIA_ERR_NETWORK",
	3: "MEDIA_ERR_DECODE",
	4: "MEDIA_ERR_SRC_NOT_SUPPORTED",
};
const observedEvents = [
	"loadstart",
	"loadedmetadata",
	"loadeddata",
	"canplay",
	"play",
	"playing",
	"pause",
	"waiting",
	"stalled",
	"suspend",
	"ended",
	"error",
];
const videoLog = document.getElementById("video-log");

function appendLog(message) {
	const timestamp = new Date().toLocaleTimeString();
	const existing =
		videoLog.textContent === "Waiting for media events."
			? ""
			: `${videoLog.textContent}\n`;
	videoLog.textContent = `${existing}[${timestamp}] ${message}`;
	videoLog.scrollTop = videoLog.scrollHeight;
}

function errorDescription(video) {
	if (!video.error) {
		return "unknown media error";
	}
	return mediaErrorNames[video.error.code] ?? `media error ${video.error.code}`;
}

for (const video of videos) {
	for (const eventName of observedEvents) {
		video.element.addEventListener(eventName, () => {
			if (eventName === "error") {
				const error = errorDescription(video.element);
				video.status.textContent = error;
				video.status.className = "video-status fail";
				appendLog(`${video.label}: ${error}`);
				return;
			}

			if (eventName === "loadedmetadata") {
				video.status.textContent = `Metadata loaded: ${video.element.videoWidth}x${video.element.videoHeight}, ${video.element.duration.toFixed(2)} seconds`;
				video.status.className = "video-status pass";
			}
			appendLog(`${video.label}: ${eventName}`);
		});
	}
}

const formats = [
	["WebM container", "video/webm"],
	["WebM VP8", 'video/webm; codecs="vp8"'],
	["WebM VP9", 'video/webm; codecs="vp9"'],
	["WebM VP9 codec string", 'video/webm; codecs="vp09.00.10.08"'],
	["MP4 container", "video/mp4"],
	["MP4 H.264 baseline", 'video/mp4; codecs="avc1.42E01E"'],
	["MP4 H.264 main", 'video/mp4; codecs="avc1.4D401E"'],
	["MP4 H.264 high", 'video/mp4; codecs="avc1.64001E"'],
];
const codecResults = document.getElementById("codec-results");
const probe = document.createElement("video");
for (const [label, mimeType] of formats) {
	const result = probe.canPlayType(mimeType);
	const row = document.createElement("tr");
	const formatCell = document.createElement("td");
	const resultCell = document.createElement("td");
	formatCell.textContent = `${label} (${mimeType})`;
	resultCell.textContent = result || "not supported";
	resultCell.className = result ? "pass" : "fail";
	row.append(formatCell, resultCell);
	codecResults.append(row);
}

async function runRangeTests() {
	const results = document.getElementById("range-results");
	results.textContent = "Running range tests...";
	const lines = [];
	for (const video of videos) {
		try {
			const response = await fetch(video.url, {
				cache: "no-store",
				headers: { Range: "bytes=0-31" },
			});
			const payload = await response.arrayBuffer();
			lines.push(
				`${video.label}: status=${response.status}, bytes=${payload.byteLength}, content-type=${response.headers.get("content-type")}, content-range=${response.headers.get("content-range")}, accept-ranges=${response.headers.get("accept-ranges")}`,
			);
		} catch (error) {
			lines.push(`${video.label}: range request failed: ${error}`);
		}
	}
	results.textContent = lines.join("\n");
}

document.getElementById("play-all").addEventListener("click", () => {
	for (const video of videos) {
		video.element
			.play()
			.catch((error) => appendLog(`${video.label}: play failed: ${error}`));
	}
});

document.getElementById("pause-all").addEventListener("click", () => {
	for (const video of videos) {
		video.element.pause();
	}
});

document.getElementById("run-range-tests").addEventListener("click", () => {
	void runRangeTests();
});
await runRangeTests();
