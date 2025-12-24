package com.algaworks.brewer.service;

import java.util.Optional;

import jakarta.persistence.PersistenceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algaworks.brewer.model.Cliente;
import com.algaworks.brewer.repository.Clientes;
import com.algaworks.brewer.service.exception.CpfCnpjClienteJaCadastradoException;
import com.algaworks.brewer.service.exception.ImpossivelExcluirEntidadeException;

@Service
public class CadastroClienteService {

	@Autowired
	private Clientes clientes;
	
	@Transactional
	public void salvar(Cliente cliente) {
		Optional<Cliente> clienteExistente = clientes.findByCpfOuCnpj(cliente.getCpfOuCnpjSemFormatacao());
		if (clienteExistente.isPresent()) {
			Cliente existente = clienteExistente.get();
			// Verifica se é um cliente diferente (não é o mesmo cliente sendo editado)
			// Se o cliente sendo salvo tem código null, é um novo cliente
			if (cliente.getCodigo() == null || !existente.getCodigo().equals(cliente.getCodigo())) {
				throw new CpfCnpjClienteJaCadastradoException("CPF/CNPJ já cadastrado");
			}
		}

		clientes.save(cliente);
	}

	@Transactional
	public void excluir(Cliente cliente) {
		try {
			clientes.delete(cliente);
			clientes.flush();
		} catch (PersistenceException e) {
			throw new ImpossivelExcluirEntidadeException("Impossível apagar cliente. Já foi usado em alguma venda.");
		}
	}

}
