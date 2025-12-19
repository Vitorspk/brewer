package com.algaworks.brewer.service;

import com.algaworks.brewer.model.Venda;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class VendaValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Venda.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmpty(errors, "cliente", "", "Selecione um cliente");

        Venda venda = (Venda) target;
        validarSeInformouItens(errors, venda);
        validarSeInformouApenasHorario(errors, venda);
    }

    private void validarSeInformouItens(Errors errors, Venda venda) {
        if (venda.getItens().isEmpty()) {
            errors.reject("", "Adicione pelo menos uma cerveja na venda");
        }
    }

    private void validarSeInformouApenasHorario(Errors errors, Venda venda) {
        if (venda.getHorarioEntrega() != null && venda.getDataEntrega() == null) {
            errors.rejectValue("dataEntrega", "", "Informe uma data de entrega");
        }
    }
}
