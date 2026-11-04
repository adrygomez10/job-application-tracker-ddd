package dev.adriangm.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DddBrowserIT extends IntegrationTest {
    @Test
    void ownerCanSaveProfileAndOpportunityOnDesktopAndMobile()
    {
        try (
            var playwright = Playwright.create();
            var browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 960))
        ) {
            loginOwner(page);
            saveProfile(page);
            capture(page, "profile");
            addOpportunity(page);
            page.getByLabel("Filtrar por empresa").fill("No match");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Aplicar filtros")).click();
            assertThat(page.locator("main").innerText()).contains("Todavía no hay oportunidades.");
            page.getByLabel("Filtrar por empresa").fill("Workshop employer");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Aplicar filtros")).click();
            assertThat(page.locator("main").innerText()).contains("Workshop employer");
            capture(page, "opportunities");
            page.navigate("http://localhost:" + port + "/integrations");
            capture(page, "integrations");
            page.navigate("http://localhost:" + port + "/notifications");
            capture(page, "notifications");
        }
    }

    private void saveProfile(Page page)
    {
        page.navigate("http://localhost:" + port + "/profile");
        page.getByLabel("Nombre visible").fill("Workshop owner");
        page.getByLabel("Puesto que buscas").fill("Java");
        page.getByLabel("Intervalo de importación (minutos)").fill("90");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Guardar perfil")).click();
        assertThat(page.getByLabel("Nombre visible").inputValue()).isEqualTo("Workshop owner");
        assertThat(page.getByLabel("Intervalo de importación (minutos)").inputValue()).isEqualTo("90");
        assertThat(page.getByLabel("Consultar Greenhouse automáticamente").isChecked()).isFalse();
    }

    private void addOpportunity(Page page)
    {
        page.navigate("http://localhost:" + port + "/opportunities");
        page.getByLabel("Empresa contratante", new Page.GetByLabelOptions().setExact(true)).fill("Workshop employer");
        page.getByLabel("Puesto", new Page.GetByLabelOptions().setExact(true)).first().fill("Java role");
        page.getByLabel("Salario bruto mínimo (opcional)").fill("45000");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Guardar oportunidad")).click();
        assertThat(page.locator("main").innerText()).contains("Workshop employer", "45000");
    }

    private static void capture(Page page, String name)
    {
        for (int width : new int[]{1440, 390}) {
            page.setViewportSize(width, 960);
            assertThat((Boolean) page.evaluate("document.documentElement.scrollWidth <= innerWidth")).isTrue();
            page
                .screenshot(
                    new Page.ScreenshotOptions()
                        .setPath(Path.of("target/screenshots/ddd-" + name + "-" + width + ".png"))
                        .setFullPage(true)
                );
        }
        page.setViewportSize(1440, 960);
    }
}
