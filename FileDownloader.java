
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

class DownloadTask {
	String fileURL;
	String saveDir;
	int priority;

	public DownloadTask(String fileURL, String saveDir, int priority) {
		this.fileURL = fileURL;
		this.saveDir = saveDir;
		this.priority = priority;
	}

	public String getFileURL() {
		return fileURL;
	}

	public String getSaveDir() {
		return saveDir;
	}

	public int getPriority() {
		return priority;
	}
}

public class FileDownloader {
	static Scanner scanner = new Scanner(System.in);

	public static void downloadFile(String fileURL, String saveDir) throws IOException {
		long downloadedBytes = 0;
		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		if (downloadedBytes > 0) {
			httpConn.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
		}
		int responseCode = httpConn.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK) {
			String fileName = "";
			String disposition = httpConn.getHeaderField("Content-Disposition");
			if (disposition != null) {
				int index = disposition.indexOf("filename=");
				if (index > 0) {
					fileName = disposition.substring(index + 10, disposition.length() - 1);
				}
			} else {
				fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
			}
			File directory = new File(saveDir);
			if (!directory.exists()) {
				directory.mkdirs();
			}
			String saveFilePath = saveDir + File.separator + fileName;

			try (InputStream inputStream = httpConn.getInputStream();
					FileOutputStream outputStream = new FileOutputStream(saveFilePath, true)) {
				int bytesRead;
				byte[] buffer = new byte[1024];
				System.out.println("Downloading...( url = " + fileURL + " )");
				while ((bytesRead = inputStream.read(buffer)) != -1) {

					outputStream.write(buffer, 0, bytesRead);
					downloadedBytes += bytesRead;
				}
				System.out.println("File downloaded successfully! ( url = " + fileURL + " )");
			}
		} else {
			System.out.println("Cannot download file. Server replied HTTP code: " + responseCode);
		}
		httpConn.disconnect();
	}

	public static void resumeDownload(String fileURL, String saveDir, long resumeByte) throws IOException {
		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestProperty("Range", "bytes=" + resumeByte + "-");
		int responseCode = httpConn.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK) {
			String fileName = "";
			String disposition = httpConn.getHeaderField("Content-Disposition");

			if (disposition != null) {
				int index = disposition.indexOf("filename=");
				if (index > 0) {
					fileName = disposition.substring(index + 10, disposition.length() - 1);
				}
			} else {
				fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
			}
			File directory = new File(saveDir);
			if (!directory.exists()) {
				directory.mkdirs();
			}
			String saveFilePath = saveDir + File.separator + fileName;
			try (InputStream inputStream = httpConn.getInputStream();
					FileOutputStream outputStream = new FileOutputStream(saveFilePath, true)) {
				int bytesRead;
				byte[] buffer = new byte[1024];
				System.out.println("File download resumed successfully!");
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
				System.out.println("File downloaded successfully! ( url = " + fileURL + " )");
			}
		} else {
			System.out.println("Cannot resume download. Server replied HTTP code: " + responseCode);
		}
		httpConn.disconnect();
	}

	private static String getDirPath(String fileURL) {
		String saveDir;
		if (fileURL.substring(fileURL.lastIndexOf(".") + 1).equals("pdf")
				|| fileURL.substring(fileURL.lastIndexOf(".") + 1).equals("txt")) {
			saveDir = "D:\\Downloads\\Documents\\";
		} else if (fileURL.substring(fileURL.lastIndexOf(".") + 1).equals("jpg")
				|| fileURL.substring(fileURL.lastIndexOf(".") + 1).equals("png")) {
			saveDir = "D:\\Downloads\\Images\\";
		} else {
			saveDir = "D:\\Downloads\\Others\\";
		}
		return saveDir;
	}

	public static void main(String[] args) throws IOException {
		System.out.println(
				"Choose a option\n1)Resume an interruped download\n2)Simultaneously download files\n3)Download files according to given priority");
		int choose = scanner.nextInt();
		if (choose == 1) {
			while (true) {
				System.out.println("Enter URL to download (or 'exit' to finish): ");
				String fileURL = scanner.next();
				if (fileURL.equalsIgnoreCase("exit")) {
					break;
				}
				String saveDir = getDirPath(fileURL);
				String fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
				String saveFilePath = saveDir + File.separator + fileName;
				File file = new File(saveFilePath);
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				long length = raf.length();
				raf.close();
				resumeDownload(fileURL, saveDir, length);
			}
		}
		if (choose == 2) {
			List<String> fileURLs = new ArrayList<>();
			while (true) {
				System.out.println("Enter URL to download (or 'exit' to finish): ");
				String fileURL = scanner.next();
				if (fileURL.equalsIgnoreCase("exit")) {
					break;
				}
				fileURLs.add(fileURL);
			}
			List<Thread> downloadThreads = new ArrayList<>();
			for (String fileURL : fileURLs) {
				Thread downloadThread = new Thread(() -> {
					try {
						downloadFile(fileURL, getDirPath(fileURL));
					} catch (IOException e) {
						System.err.println("Error downloading file: " + fileURL);
					}
				});
				downloadThreads.add(downloadThread);
				downloadThread.start();
			}
			for (Thread thread : downloadThreads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					System.err.println("Thread interrupted: " + e.getMessage());
				}
			}
			System.out.println("All files downloaded successfully!");
		}
		if (choose == 3) {
			PriorityQueue<DownloadTask> downloadQueue = new PriorityQueue<>(
					Comparator.comparingInt(DownloadTask::getPriority));
			while (true) {
				System.out.println("Enter URL to download (or 'exit' to finish): ");
				String fileURL = scanner.next();
				if (fileURL.equalsIgnoreCase("exit")) {
					break;
				}
				String saveDir = getDirPath(fileURL);
				System.out.println("Enter priority for the download (lower value means higher priority): ");
				int priority = scanner.nextInt();
				scanner.nextLine();

				DownloadTask task = new DownloadTask(fileURL, saveDir, priority);
				downloadQueue.offer(task);
			}
			while (!downloadQueue.isEmpty()) {
				DownloadTask task = downloadQueue.poll();
				try {
					downloadFile(task.getFileURL(), task.getSaveDir());
				} catch (IOException e) {
					System.err.println("Error downloading file: " + task.getFileURL());
				}
			}
		}
		scanner.close();
	}
}