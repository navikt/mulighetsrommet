import {
  FileUploadHandler,
  type FileUpload as FileUploadType,
  parseFormData,
} from "@mjackson/form-data-parser";
import { ActionFunctionArgs } from "react-router";
import { FileUploader } from "../components/fileUploader/FileUploader";

const MAX_SIZE = 1024 * 1024 * 3; // 3MB
const MAX_SIZE_MB = MAX_SIZE / 1024 / 1024;
const MAX_FILES = 10;

const uploadHandler: FileUploadHandler = async (fileUpload: FileUploadType) => {
  console.log("Inside upload handler");
  if (fileUpload.fieldName === "vedlegg") {
    // process the upload and return a File
    const bytes = await fileUpload.bytes();
    console.log("File name:", fileUpload.name);
    console.log("File type:", fileUpload.type);
    console.log("File content:", new TextDecoder().decode(bytes));
    console.log("File size in bytes:", bytes.length);
    console.log("Field name:", fileUpload.fieldName);
    return fileUpload;
  }
};

export async function action({ request }: ActionFunctionArgs) {
  console.log("Inside action");
  const formData = await parseFormData(request, uploadHandler);
  const fileNames = formData.getAll("vedlegg") as string[];

  // Here you would typically upload the files to your storage service
  // For now, we'll just return the file names
  return {
    success: true,
    files: fileNames,
  };
}

export default function VedleggPage() {
  return <FileUploader maxFiles={MAX_FILES} maxSizeMB={MAX_SIZE_MB} />;
}
