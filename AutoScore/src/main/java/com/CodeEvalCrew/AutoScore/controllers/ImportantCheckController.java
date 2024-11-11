import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
import com.CodeEvalCrew.AutoScore.services.check_important.ICheckImportant;


@RestController
@RequestMapping("/api/autoscore-check-important")
public class ImportantCheckController {
    @Autowired
    private ICheckImportant checkImportant;

    @PostMapping("")
    public ResponseEntity<?> checkImportant(@RequestBody CheckImportantRequest request) {
        try {
            var result = checkImportant.checkImportantForGranding(request);

        }
    }
 }
