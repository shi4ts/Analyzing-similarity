package com.koval.resolver.processor.documentation.core;

import com.koval.resolver.processor.documentation.bean.MediaType;


public class DocTypeDetector {

  public MediaType detectType(final String fileName) {
    if (fileName.endsWith(".pdf")) {
      return MediaType.PDF;
    } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
      return MediaType.WORD;
    }
    return MediaType.UNKNOWN;
  }
}
