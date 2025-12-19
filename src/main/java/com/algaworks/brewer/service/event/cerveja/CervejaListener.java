package com.algaworks.brewer.service.event.cerveja;

import org.springframework.stereotype.Component;

@Component
public class CervejaListener {

	// Photo upload is handled directly by FotosController
	// The CervejaSalvaEvent is published after the cerveja is saved
	// At this point, the foto field contains only the filename (String), not MultipartFile[]
	// Therefore, no action is needed in this listener for photo handling

}
